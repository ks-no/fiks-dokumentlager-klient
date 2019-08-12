package no.ks.fiks.dokumentlager.klient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.authentication.AuthenticationStrategy;
import no.ks.fiks.dokumentlager.klient.model.*;
import no.ks.fiks.dokumentlager.klient.path.DefaultPathHandler;
import no.ks.fiks.dokumentlager.klient.path.PathHandler;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("WeakerAccess")
public class DokumentlagerApiImpl implements DokumentlagerApi {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(new SslContextFactory.Client());

    private final String uploadbaseUrl;
    private final String downloadBaseUrl;
    private final AuthenticationStrategy authenticationStrategy;
    private final PathHandler pathHandler;

    private final Function<Request, Request> requestInterceptor;

    public DokumentlagerApiImpl(@NonNull String uploadbaseUrl,
                                @NonNull String downloadBaseUrl,
                                @NonNull AuthenticationStrategy authenticationStrategy,
                                @NonNull Function<Request, Request> requestInterceptor) {
        this(uploadbaseUrl, downloadBaseUrl, authenticationStrategy, requestInterceptor, new DefaultPathHandler());
    }

    public DokumentlagerApiImpl(@NonNull String uploadBaseUrl,
                                @NonNull String downloadBaseUrl,
                                @NonNull AuthenticationStrategy authenticationStrategy,
                                @NonNull Function<Request, Request> requestInterceptor,
                                @NonNull PathHandler pathHandler) {
        this.uploadbaseUrl = uploadBaseUrl;
        this.downloadBaseUrl = downloadBaseUrl;
        this.authenticationStrategy = authenticationStrategy;
        this.requestInterceptor = requestInterceptor;
        this.pathHandler = pathHandler;

        try {
            this.client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<DokumentMetadataUploadResult> uploadDokument(@NonNull InputStream dokumentStream,
                                                                              @NonNull DokumentMetadataUpload metadata,
                                                                              @NonNull UUID fiksOrganisasjonId,
                                                                              @NonNull UUID kontoId,
                                                                              boolean kryptert) {
        log.debug("Uploading {}dokument for organisasjon {} and konto {}: {}", kryptert ? "encrypted " : "", fiksOrganisasjonId, kontoId, metadata);
        try {
            MultiPartContentProvider multipart = new MultiPartContentProvider();
            multipart.addFieldPart("metadata", new StringContentProvider(objectMapper.writeValueAsString(metadata)), null);
            multipart.addFilePart("dokument", metadata.getDokumentnavn(), new InputStreamContentProvider(dokumentStream), null);
            multipart.close();

            InputStreamResponseListener listener = new InputStreamResponseListener();
            newUploadRequest()
                    .method(HttpMethod.POST)
                    .path(pathHandler.getUploadPath(fiksOrganisasjonId, kontoId))
                    .param("kryptert", String.valueOf(kryptert))
                    .content(multipart)
                    .send(listener);

            Response response = listener.get(1, TimeUnit.HOURS);
            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = IOUtils.toString(listener.getInputStream(), StandardCharsets.UTF_8);
                throw new DokumentlagerHttpException(String.format("HTTP-error during upload (%d): %s", status, content), status, content);
            }

            return buildResponse(response, objectMapper.readValue(listener.getInputStream(), DokumentMetadataUploadResult.class));
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse deleteDokument(@NonNull UUID fiksOrganisasjonId,
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

                Response response = listener.get(1, TimeUnit.HOURS);
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

            return buildResponse(response, objectMapper.readValue(response.getContentAsString(), DokumentMetadataDownloadResult.class));
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

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
        return httpStatus >= 400 && httpStatus < 600;
    }

    private <T> DokumentlagerResponse<T> buildResponse(Response response, T result) {
        return DokumentlagerResponse.<T>builder()
                .result(result)
                .httpStatus(response.getStatus())
                .httpHeaders(response.getHeaders().stream().collect(Collectors.toMap(HttpField::getName, HttpField::getValue)))
                .build();
    }

    private Request newUploadRequest() {
        return newRequest(uploadbaseUrl);
    }

    private Request newDownloadRequest() {
        return newRequest(downloadBaseUrl);
    }

    private Request newRequest(String baseUrl) {
        Request request = client.newRequest(baseUrl)
                .onRequestBegin(authenticationStrategy::setAuthenticationHeaders);
        return requestInterceptor.apply(request);
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
