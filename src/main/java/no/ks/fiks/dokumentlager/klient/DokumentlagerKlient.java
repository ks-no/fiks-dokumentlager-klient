package no.ks.fiks.dokumentlager.klient;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentlagerResponse;
import no.ks.kryptering.CMSKrypteringImpl;
import no.ks.kryptering.CMSStreamKryptering;
import org.eclipse.jetty.http.HttpStatus;

import java.io.*;
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
public class DokumentlagerKlient implements Closeable {

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
        Future krypteringFuture = null;
        InputStream inputStream = dokumentStream;

        if (metadata.getSikkerhetsniva() != null && metadata.getSikkerhetsniva() > 3 && !skalKrypteres) {
            log.info("Dokument will be encrypted as sikkerhetsnivå is greater than 3");
            skalKrypteres = true;
        }

        if (skalKrypteres) {
            try {
                if (publicCertificate == null) {
                    publicCertificate = getPublicKeyAsX509Certificate().getResult();
                }

                DokumentlagerPipedInputStream pipedInputStream = new DokumentlagerPipedInputStream();
                PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
                inputStream = pipedInputStream;

                krypteringFuture = executor.submit(() -> {
                    try {
                        log.debug("Starting encryption...");
                        kryptering.krypterData(pipedOutputStream, dokumentStream, publicCertificate, provider);
                        log.debug("Encryption completed");
                    } catch (Exception e) {
                        log.error("Encryption failed, setting exception on encrypted InputStream");
                        pipedInputStream.setException(e);
                    } finally {
                        try {
                            log.debug("Closing encryption OutputStream");
                            pipedOutputStream.close();
                            log.debug("Encryption OutputStream closed");
                        } catch (IOException e) {
                            log.error("Failed closing encryption OutputStream", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.debug("Starting upload...");
        DokumentlagerResponse<DokumentMetadataUploadResult> response = api.uploadDokument(inputStream, metadata, fiksOrganisasjonId, kontoId, skalKrypteres);
        log.debug("Upload completed");

        if (krypteringFuture != null) {
            try {
                log.debug("Waiting for encryption thread to terminate...");
                krypteringFuture.get(10, TimeUnit.SECONDS);
                log.debug("Encryption thread terminated");
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

    @Override
    public void close() throws IOException {
        api.close();
        executor.shutdown();
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
