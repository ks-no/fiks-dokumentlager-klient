package no.ks.fiks.dokumentlager.klient;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.authentication.AuthenticationStrategy;
import no.ks.fiks.dokumentlager.klient.model.*;
import no.ks.fiks.dokumentlager.klient.path.DefaultPathHandler;
import no.ks.fiks.dokumentlager.klient.path.PathHandler;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ContentType;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class DokumentlagerApiImpl implements DokumentlagerApi {

    private static final String KRYPTERT_PARAM = "kryptert";
    private static final String METADATA_PART = "metadata";
    private static final String DOKUMENT_PART = "dokument";

    private final JsonMapper mapper = new JsonMapper();

    private final HttpClient client;
    private final Duration uploadTimeout;
    private final Duration downloadTimeout;

    private final String uploadbaseUrl;
    private final String downloadBaseUrl;
    private final AuthenticationStrategy authenticationStrategy;
    private final PathHandler pathHandler;

    private final Function<Request, Request> requestInterceptor;

    private DokumentlagerApiImpl(@NonNull String uploadBaseUrl,
                                 @NonNull String downloadBaseUrl,
                                 @NonNull AuthenticationStrategy authenticationStrategy,
                                 Function<Request, Request> requestInterceptor,
                                 @NonNull PathHandler pathHandler,
                                 @NonNull HttpConfiguration httpConfiguration) {
        this.uploadbaseUrl = uploadBaseUrl;
        this.downloadBaseUrl = downloadBaseUrl;
        this.authenticationStrategy = authenticationStrategy;
        this.requestInterceptor = requestInterceptor;
        this.pathHandler = pathHandler;

        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(new SslContextFactory.Client());
        this.client = new HttpClient(new HttpClientTransportDynamic(clientConnector));
        this.client.setIdleTimeout(httpConfiguration.getIdleTimeout().toMillis());
        this.uploadTimeout = httpConfiguration.getUploadTimeout();
        this.downloadTimeout = httpConfiguration.getDownloadTimeout();

        try {
            this.client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DokumentlagerApiImplBuilder builder() {
        return new DokumentlagerApiImplBuilder();
    }

    @Override
    public DokumentlagerResponse<DokumentMetadataUploadResult> uploadDokument(@NonNull InputStream dokumentStream,
                                                                              @NonNull DokumentMetadataUpload metadata,
                                                                              @NonNull UUID fiksOrganisasjonId,
                                                                              @NonNull UUID kontoId,
                                                                              boolean kryptert) {
        log.debug("Uploading {}dokument for organisasjon {} and konto {}: {}", kryptert ? "encrypted " : "", fiksOrganisasjonId, kontoId, metadata);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.POST)
                    .path(pathHandler.getUploadPath(fiksOrganisasjonId, kontoId))
                    .param(KRYPTERT_PARAM, String.valueOf(kryptert))
                    .body(buildMultipartContent(metadata, dokumentStream))
                    .timeout(uploadTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .send();

            if (isError(response.getStatus())) {
                log.info("Upload request failed with status {}", response.getStatus());
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(String.format("HTTP-error during upload (%d): %s", status, content), status, content);
            }

            return buildResponse(response, mapper.fromJson(response.getContent(), DokumentMetadataUploadResult.class));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            if(e.getMessage().equals("java.io.IOException: Exceeded configured input limit")) {
                throw new DokumentlagerIOException(e.getMessage(), e);
            }
            throw new RuntimeException(e);
        }
    }

    private MultiPartRequestContent buildMultipartContent(DokumentMetadataUpload metadata, InputStream dokumentStream) {
        MultiPartRequestContent multipart = new MultiPartRequestContent();
        addMetadataPart(multipart, metadata);
        addDokumentPart(multipart, metadata.getDokumentnavn(), dokumentStream);
        multipart.close();
        return multipart;
    }

    private void addMetadataPart(MultiPartRequestContent multipart, DokumentMetadataUpload metadata) {
        addPart(
                multipart,
                new MultiPart.ContentSourcePart(
                        METADATA_PART,
                        null,
                        HttpFields.from(
                                new HttpField(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        ),
                        new StringRequestContent(mapper.toJson(metadata))
                )
        );
    }

    private void addDokumentPart(MultiPartRequestContent multipart, String fileName, InputStream dokumentStream) {
        addPart(
                multipart,
                new MultiPart.ContentSourcePart(
                        DOKUMENT_PART,
                        fileName,
                        HttpFields.from(
                                new HttpField(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.getMimeType())
                        ),
                        new InputStreamRequestContent(dokumentStream)
                )
        );
    }

    private void addPart(MultiPartRequestContent multipart, MultiPart.Part part) {
        boolean added = multipart.addPart(part);
        if (!added) {
            throw new RuntimeException("Failed to add multipart");
        }
    }

    @Override
    public DokumentlagerResponse<DokumentMetadataUpdateResult> updateDokumentMetadata(UUID fiksOrganisasjonId, UUID kontoId, UUID dokumentId, DokumentMetadataUpdate update) {
        log.debug("Updating metadata for dokument with id {} for organisasjon {} and konto {}", dokumentId, fiksOrganisasjonId, kontoId);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.PATCH)
                    .path(pathHandler.getUpdateMetadataPath(fiksOrganisasjonId, kontoId, dokumentId))
                    .body(createJsonBody(update))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error during metadata update (%d): %s", status, content), status, content);
            }

            return buildResponse(response, mapper.fromJson(response.getContent(), DokumentMetadataUpdateResult.class));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<Void> deleteDokument(@NonNull UUID fiksOrganisasjonId,
                                                      @NonNull UUID kontoId,
                                                      @NonNull UUID dokumentId) {
        log.debug("Deleting dokument with id {} for organisasjon {} and konto {}", dokumentId, fiksOrganisasjonId, kontoId);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.DELETE)
                    .path(pathHandler.getDeletePath(fiksOrganisasjonId, kontoId, dokumentId))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error during delete (%d): %s", status, content), status, content);
            }

            return buildResponse(response, null);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<Void> deleteDokumenterByKorrelasjonsid(UUID fiksOrganisasjonId, UUID kontoId, UUID korrelasjonsid) {
        log.debug("Deleting dokumenter with korrelasjonsid {} for organisasjon {} and konto {}", korrelasjonsid, fiksOrganisasjonId, kontoId);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.DELETE)
                    .path(pathHandler.getDeleteByKorrelasjonsidPath(fiksOrganisasjonId, kontoId, korrelasjonsid))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error during delete by korrelasjonsid (%d): %s", status, content), status, content);
            }

            return buildResponse(response, null);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<InputStream> downloadDokument(@NonNull UUID dokumentId) {
        return createDownloadRequestSupplier(dokumentId).get();
    }

    @Override
    public DokumentlagerResponse<InputStream> downloadDokumentLazy(@NonNull UUID dokumentId) {
        log.debug("Initialized lazy download for dokument {} ", dokumentId);
        Supplier<DokumentlagerResponse<InputStream>> downloadRequestSupplier = createDownloadRequestSupplier(dokumentId);
        return new LazyDokumentlagerResponse(downloadRequestSupplier::get);
    }

    private Supplier<DokumentlagerResponse<InputStream>> createDownloadRequestSupplier(@NonNull UUID dokumentId) {
        Request request = newDownloadRequest()
                .method(HttpMethod.GET)
                .path(pathHandler.getDownloadPath(dokumentId));

        return () -> {
            try {
                log.debug("Downloading dokument {}", dokumentId);

                InputStreamResponseListener listener = new InputStreamResponseListener();
                request.send(listener);

                Response response = listener.get(downloadTimeout.toMillis(), TimeUnit.MILLISECONDS);
                if (isError(response.getStatus())) {
                    int status = response.getStatus();
                    String content = IOUtils.toString(listener.getInputStream(), StandardCharsets.UTF_8);
                    throw new DokumentlagerHttpException(
                            String.format("HTTP-error during download (%d): %s", status, content), status, content);
                }

                return buildResponse(response, listener.getInputStream());
            } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public DokumentlagerResponse<DokumentMetadataDownloadResult> downloadDokumentMetadata(@NonNull UUID dokumentId) {
        log.debug("Downloading metadata for dokument with id {}", dokumentId);
        try {
            ContentResponse response = newDownloadRequest()
                    .method(HttpMethod.GET)
                    .path(pathHandler.getDownloadMetadataPath(dokumentId))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error during download (%d): %s", status, content), status, content);
            }

            return buildResponse(response, mapper.fromJson(response.getContent(), DokumentMetadataDownloadResult.class));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<Sokeresultat> sokDokumenterMedKorrelasjonsid(UUID fiksOrganisasjonId, UUID kontoId, UUID korrelasjonsid, Integer fra, Integer til) {
        log.debug("Search documents with correlationid {}", korrelasjonsid);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.POST)
                    .path(pathHandler.getQueryDocumentPath(fiksOrganisasjonId, kontoId))
                    .param("fra", String.valueOf(fra))
                    .param("til", String.valueOf(til))
                    .body(createJsonBody(new Korrelasjonsid(korrelasjonsid)))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error during document query (%d): %s", status, content), status, content);
            }

            return buildResponse(response, mapper.fromJson(response.getContent(), Sokeresultat.class));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private record Korrelasjonsid(UUID korrelasjonsid) {}

    @Override
    public DokumentlagerResponse<String> getPublicKey() {
        log.debug("Getting public key");
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.GET)
                    .path(pathHandler.getPublicKeyPath())
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-error getting public-key (%d): %s", status, content), status, content);
            }

            return buildResponse(response, response.getContentAsString());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isError(int httpStatus) {
        return HttpStatus.isClientError(httpStatus) || HttpStatus.isServerError(httpStatus);
    }

    private <T> DokumentlagerResponse<T> buildResponse(Response response, T result) {
        return DokumentlagerResponse.<T>builder()
                .result(result)
                .httpStatus(response.getStatus())
                .httpHeaders(response.getHeaders().stream().collect(Collectors.toMap(HttpField::getName, HttpField::getValue, (prev, next) -> next, HashMap::new)))
                .build();
    }

    private Request newUploadRequest() {
        return newRequest(uploadbaseUrl);
    }

    private Request newDownloadRequest() {
        return newRequest(downloadBaseUrl);
    }

    private Request newRequest(String baseUrl) {
        Request request = client.newRequest(baseUrl);

        authenticationStrategy.setAuthenticationHeaders(request);

        if (requestInterceptor != null) {
            return requestInterceptor.apply(request);
        }
        return request;
    }

    private StringRequestContent createJsonBody(Object body) {
        return new StringRequestContent("application/json", mapper.toJson(body), StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class DokumentlagerApiImplBuilder {

        private String uploadBaseUrl;
        private String downloadBaseUrl;
        private AuthenticationStrategy authenticationStrategy;
        private PathHandler pathHandler;
        private Function<Request, Request> requestInterceptor;
        private HttpConfiguration httpConfiguration;

        public DokumentlagerApiImplBuilder uploadBaseUrl(String uploadBaseUrl) {
            this.uploadBaseUrl = uploadBaseUrl;
            return this;
        }

        public DokumentlagerApiImplBuilder downloadBaseUrl(String downloadBaseUrl) {
            this.downloadBaseUrl = downloadBaseUrl;
            return this;
        }

        public DokumentlagerApiImplBuilder authenticationStrategy(AuthenticationStrategy authenticationStrategy) {
            this.authenticationStrategy = authenticationStrategy;
            return this;
        }

        public DokumentlagerApiImplBuilder pathHandler(PathHandler pathHandler) {
            this.pathHandler = pathHandler;
            return this;
        }

        public DokumentlagerApiImplBuilder requestInterceptor(Function<Request, Request> requestInterceptor) {
            this.requestInterceptor = requestInterceptor;
            return this;
        }

        public DokumentlagerApiImplBuilder httpConfiguration(HttpConfiguration httpConfiguration) {
            this.httpConfiguration = httpConfiguration;
            return this;
        }

        public DokumentlagerApiImpl build() {
            if (pathHandler == null) {
                pathHandler = new DefaultPathHandler();
            }
            if (httpConfiguration == null) {
                httpConfiguration = HttpConfiguration.builder().build();
            }
            return new DokumentlagerApiImpl(uploadBaseUrl, downloadBaseUrl, authenticationStrategy, requestInterceptor, pathHandler, httpConfiguration);
        }

    }
}
