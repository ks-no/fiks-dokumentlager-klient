package no.ks.fiks.dokumentlager.klient;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.model.*;
import no.ks.kryptering.CMSKrypteringImpl;
import no.ks.kryptering.CMSStreamKryptering;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

import java.io.*;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@SuppressWarnings("WeakerAccess")
public class DokumentlagerKlient implements Closeable {

    private static final int END_OF_STREAM = -1;
    private final Provider provider = Security.getProvider("BC");
    private X509Certificate publicCertificate = null;

    private final DokumentlagerApi api;
    private final ExecutorService executor;
    private final CMSStreamKryptering kryptering;

    private DokumentlagerKlient(@NonNull DokumentlagerApi dokumentlagerApi,
                                @NonNull ExecutorService executor,
                                @NonNull CMSStreamKryptering kryptering) {
        this.api = dokumentlagerApi;
        this.executor = executor;
        this.kryptering = kryptering;
    }

    public static DokumentlagerKlientBuilder builder() {
        return new DokumentlagerKlientBuilder();
    }

    public DokumentlagerResponse<DokumentMetadataUploadResult> upload(@NonNull InputStream dokumentStream,
                                                                      @NonNull DokumentMetadataUpload metadata,
                                                                      @NonNull UUID fiksOrganisasjonId,
                                                                      @NonNull UUID kontoId) {
        return upload(dokumentStream, metadata, fiksOrganisasjonId, kontoId, false);
    }

    public DokumentlagerResponse<DokumentMetadataUploadResult> uploadAlreadyEncrypted(@NonNull InputStream dokumentStream,
                                                                                      @NonNull DokumentMetadataUpload metadata,
                                                                                      @NonNull UUID fiksOrganisasjonId,
                                                                                      @NonNull UUID kontoId) {
        log.debug("Starting upload...");
        DokumentlagerResponse<DokumentMetadataUploadResult> response = api.uploadDokument(dokumentStream, metadata, fiksOrganisasjonId, kontoId, true);
        log.debug("Upload completed");
        return response;
    }

    public DokumentlagerResponse<DokumentMetadataUploadResult> upload(@NonNull InputStream dokumentStream,
                                                                      @NonNull DokumentMetadataUpload metadata,
                                                                      @NonNull UUID fiksOrganisasjonId,
                                                                      @NonNull UUID kontoId,
                                                                      boolean skalKrypteres) {
        Future<?> krypteringFuture = null;
        InputStream inputStream = dokumentStream;

        if (metadata.getSikkerhetsniva() != null && metadata.getSikkerhetsniva() > 3 && !skalKrypteres) {
            log.info("Dokument will be encrypted as sikkerhetsnivå is greater than 3");
            skalKrypteres = true;
        }

        try (DokumentlagerPipedInputStream pipedInputStream = new DokumentlagerPipedInputStream();
             PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream)) {

            if (skalKrypteres) {
                if (publicCertificate == null) {
                    publicCertificate = getPublicKeyAsX509Certificate().getResult();
                }

                inputStream = pipedInputStream;
                krypteringFuture = executor.submit(() -> krypter(dokumentStream, pipedInputStream, pipedOutputStream, MDC.getCopyOfContextMap()));
            }

            try (PushbackInputStream pis = new PushbackInputStream(inputStream)) {
                int read = pis.read();
                if(read == END_OF_STREAM){
                    throw new DokumentlagerBadRequestException("Dokument uten innhold kan ikke lastes opp");
                }
                pis.unread(read);
                DokumentlagerResponse<DokumentMetadataUploadResult> response = api.uploadDokument(pis, metadata, fiksOrganisasjonId, kontoId, skalKrypteres);
                log.debug("Upload completed");

                if (krypteringFuture != null) {
                    log.debug("Waiting for encryption thread to terminate...");
                    krypteringFuture.get(10, TimeUnit.SECONDS);
                    log.debug("Encryption thread terminated");
                }
                return response;
            }
        }  catch (IOException e){
            throw new DokumentlagerIOException(e.getMessage(),e);
        }  catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Upload failed", e);
            throw new RuntimeException(e);
        }
        finally {
            avbrytKrypteringFuture(krypteringFuture);
        }
    }

    private static void avbrytKrypteringFuture(Future<?> krypteringFuture) {
        if (krypteringFuture != null && !krypteringFuture.isCancelled()) {
            log.debug("Cancelling encryption future");
            boolean cancelled = krypteringFuture.cancel(true);
            log.info("Encryption future cancelled, result: {}", cancelled);
        }
    }

    private void krypter(@NotNull InputStream dokumentStream, DokumentlagerPipedInputStream pipedInputStream, PipedOutputStream pipedOutputStream, Map<String, String> contextMap) {
        Optional.ofNullable(contextMap).ifPresent(MDC::setContextMap);
        try {
            log.debug("Starting encryption...");
            kryptering.krypterData(pipedOutputStream, dokumentStream, publicCertificate, provider);
            log.info("Encryption completed...");
        } catch (Exception e) {
            log.error("Encryption failed, setting exception on encrypted InputStream", e);
            pipedInputStream.setException(e);
        } finally {
            try {
                log.debug("Closing encryption OutputStream");
                pipedOutputStream.close();
                log.debug("Encryption OutputStream closed");
            } catch (IOException e) {
                log.error("Failed closing encryption OutputStream", e);
                throw new RuntimeException(e);
            } finally {
                MDC.clear();
            }
        }
    }

    public DokumentlagerResponse delete(@NonNull UUID fiksOrganisasjonId,
                                        @NonNull UUID kontoId,
                                        @NonNull UUID dokumentId) {
        return api.deleteDokument(fiksOrganisasjonId, kontoId, dokumentId);
    }

    public DokumentlagerResponse<String> getPublicKey() {
        return api.getPublicKey();
    }

    public DokumentlagerResponse<X509Certificate> getPublicKeyAsX509Certificate() {
        DokumentlagerResponse<String> publicKeyResponse = getPublicKey();
        String publicKey = publicKeyResponse.getResult();

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Base64.Decoder base64 = Base64.getMimeDecoder();

            byte[] buffer = base64.decode(
                    publicKey.replace("-----BEGIN CERTIFICATE-----", "")
                            .replace("-----END CERTIFICATE-----", ""));

            return DokumentlagerResponse.<X509Certificate>builder()
                    .httpStatus(publicKeyResponse.getHttpStatus())
                    .result((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)))
                    .build();

        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

    }

    public DokumentlagerResponse<InputStream> download(@NonNull UUID dokumentId) {
        return api.downloadDokument(dokumentId);
    }

    public DokumentlagerResponse<InputStream> downloadLazy(@NonNull UUID dokumentId) {
        return api.downloadDokumentLazy(dokumentId);
    }

    public DokumentlagerResponse<DokumentMetadataDownloadResult> downloadMetadata(@NonNull UUID dokumentId) {
        return api.downloadDokumentMetadata(dokumentId);
    }

    public DokumentlagerResponse<Sokeresultat> sokDokumenterMedKorrelasjonsid(UUID fiksOrganisasjonId,
                                                                              UUID kontoId,
                                                                              UUID korrelasjonsid,
                                                                              Integer fra,
                                                                              Integer til) {
        return api.sokDokumenterMedKorrelasjonsid(fiksOrganisasjonId, kontoId, korrelasjonsid, fra, til);
    }

    @Override
    public void close() throws IOException {
        api.close();
        executor.shutdown();
    }

    public static class DokumentlagerKlientBuilder {

        private static final int DEFAULT_THREAD_POOL_SIZE = 4;

        private DokumentlagerApi api;
        private CMSStreamKryptering kryptering;
        private ExecutorService executor;

        private DokumentlagerKlientBuilder() {
        }

        public DokumentlagerKlientBuilder api(DokumentlagerApi api) {
            this.api = api;
            return this;
        }

        public DokumentlagerKlientBuilder kryptering(CMSStreamKryptering kryptering) {
            this.kryptering = kryptering;
            return this;
        }

        public DokumentlagerKlientBuilder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public DokumentlagerKlient build() {
            if (kryptering == null) {
                kryptering = new CMSKrypteringImpl();
            }
            if (executor == null) {
                executor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
            }
            return new DokumentlagerKlient(api, executor, kryptering);
        }
    }
}
