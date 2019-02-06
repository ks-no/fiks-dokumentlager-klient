package no.ks.fiks.dokumentlager.klient;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentlagerResponse;
import no.ks.kryptering.CMSKrypteringImpl;
import no.ks.kryptering.CMSStreamKryptering;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@SuppressWarnings("WeakerAccess")
public class DokumentlagerKlient {

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

    public DokumentlagerResponse<DokumentMetadataUploadResult> upload(@NonNull InputStream dokumentStream,
                                                                      @NonNull DokumentMetadataUpload metadata,
                                                                      @NonNull UUID fiksOrganisasjonId,
                                                                      @NonNull UUID kontoId,
                                                                      boolean skalKrypteres) {
        Future krypteringFuture = null;
        InputStream inputStream = dokumentStream;

        if (metadata.getSikkerhetsniva() != null && metadata.getSikkerhetsniva() > 3 && !skalKrypteres) {
            log.info("Dokumentet vil bli kryptert siden sikkerhetsnivå er høyere enn 3");
            skalKrypteres = true;
        }

        if (skalKrypteres) {
            try {
                DokumentlagerPipedInputStream pipedInputStream = new DokumentlagerPipedInputStream();
                PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
                inputStream = pipedInputStream;

                krypteringFuture = executor.submit(() -> {
                    try {
                        log.debug("Starter kryptering...");
                        kryptering.krypterData(pipedOutputStream, dokumentStream, getX509Certificate(), provider);
                        log.debug("Kryptering ferdig");
                    } catch (Exception e) {
                        log.error("Kryptering feilet, setter exception på kryptert InputStream");
                        pipedInputStream.setException(e);
                    } finally {
                        try {
                            log.debug("Lukker OutputStream for kryptering");
                            pipedOutputStream.close();
                            log.debug("OutputStream for kryptering lukket");
                        } catch (IOException e) {
                            log.error("Klarte ikke å lukke OutputStream for kryptering", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.debug("Starter opplasting...");
        DokumentlagerResponse<DokumentMetadataUploadResult> response = api.uploadDokument(inputStream, metadata, fiksOrganisasjonId, kontoId, skalKrypteres);
        log.debug("Opplasting ferdig");

        if (krypteringFuture != null) {
            try {
                log.debug("Venter på at krypteringstråd skal terminere...");
                krypteringFuture.get(10, TimeUnit.SECONDS);
                log.debug("Krypteringstråd terminert");
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

        return response;
    }

    public DokumentlagerResponse delete(@NonNull UUID fiksOrganisasjonId,
                                        @NonNull UUID kontoId,
                                        @NonNull UUID dokumentId) {
        return api.deleteDokument(fiksOrganisasjonId, kontoId, dokumentId);
    }

    private X509Certificate getX509Certificate() {
        if (publicCertificate == null) {
            String publicKey = api.getPublicKey().getResult();

            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Base64.Decoder base64 = Base64.getMimeDecoder();

                byte[] buffer = base64.decode(
                        publicKey.replace("-----BEGIN CERTIFICATE-----", "")
                                .replace("-----END CERTIFICATE-----", ""));

                this.publicCertificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer));

            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
        }
        return publicCertificate;
    }

    public DokumentlagerResponse<InputStream> download(@NonNull UUID dokumentId) {
        return api.downloadDokument(dokumentId);
    }

    public static class DokumentlagerKlientBuilder {
        private DokumentlagerApi api;
        private CMSStreamKryptering kryptering = new CMSKrypteringImpl();
        private int threadPoolSize = 4;

        private DokumentlagerKlientBuilder() { }

        public DokumentlagerKlientBuilder api(DokumentlagerApi api) {
            this.api = api;
            return this;
        }

        public DokumentlagerKlientBuilder kryptering(CMSStreamKryptering kryptering) {
            this.kryptering = kryptering;
            return this;
        }

        public DokumentlagerKlientBuilder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public DokumentlagerKlient build() {
            return new DokumentlagerKlient(api, Executors.newFixedThreadPool(threadPoolSize), kryptering);
        }
    }
}
