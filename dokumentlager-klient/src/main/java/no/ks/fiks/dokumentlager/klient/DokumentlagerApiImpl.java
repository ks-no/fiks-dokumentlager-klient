package no.ks.fiks.dokumentlager.klient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ks.fiks.dokumentlager.klient.authentication.AuthenticationStrategy;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentlagerResponse;
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
import org.slf4j.MDC;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("WeakerAccess")
public class DokumentlagerApiImpl implements DokumentlagerApi {

    public static final String REQUEST_ID_HEADER = "requestid";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = new HttpClient();

    private final String uploadbaseUrl;
    private final String downloadBaseUrl;
    private final AuthenticationStrategy authenticationStrategy;
    private final PathHandler pathHandler;

    public DokumentlagerApiImpl(@NonNull String uploadbaseUrl,
                                @NonNull String downloadBaseUrl,
                                @NonNull AuthenticationStrategy authenticationStrategy) {
        this(uploadbaseUrl, downloadBaseUrl, authenticationStrategy, new DefaultPathHandler());
    }

    public DokumentlagerApiImpl(@NonNull String uploadBaseUrl,
                                @NonNull String downloadBaseUrl,
                                @NonNull AuthenticationStrategy authenticationStrategy,
                                @NonNull PathHandler pathHandler) {
        this.uploadbaseUrl = uploadBaseUrl;
        this.downloadBaseUrl = downloadBaseUrl;
        this.authenticationStrategy = authenticationStrategy;
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
        log.debug("Laster opp {}dokument for organisasjon {} og konto {}: {}", kryptert ? "kryptert " : "", fiksOrganisasjonId, kontoId, metadata);
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
                throw new DokumentlagerHttpException(String.format("HTTP-feil under opplasting (%d): %s", status, content), status, content);
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
        log.debug("Sletter dokument med id {} for organisasjon {} og konto {}", dokumentId, fiksOrganisasjonId, kontoId);
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.DELETE)
                    .path(pathHandler.getDeletePath(fiksOrganisasjonId, kontoId, dokumentId))
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-feil under sletting (%d): %s", status, content), status, content);
            }

            return buildResponse(response, null);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<InputStream> downloadDokument(@NonNull UUID dokumentId) {
        log.debug("Laster ned dokument {}", dokumentId);
        try {
            InputStreamResponseListener listener = new InputStreamResponseListener();
            newDownloadRequest()
                    .method(HttpMethod.GET)
                    .path(pathHandler.getDownloadPath(dokumentId))
                    .send(listener);

            Response response = listener.get(1, TimeUnit.HOURS);
            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = IOUtils.toString(listener.getInputStream(), StandardCharsets.UTF_8);
                throw new DokumentlagerHttpException(
                        String.format("HTTP-feil under nedlasting (%d): %s", status, content), status, content);
            }

            return buildResponse(response, listener.getInputStream());
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DokumentlagerResponse<String> getPublicKey() {
        log.debug("Henter public key");
        try {
            ContentResponse response = newUploadRequest()
                    .method(HttpMethod.GET)
                    .path(pathHandler.getPublicKeyPath())
                    .send();

            if (isError(response.getStatus())) {
                int status = response.getStatus();
                String content = response.getContentAsString();
                throw new DokumentlagerHttpException(
                        String.format("HTTP-feil under henting av public-key (%d): %s", status, content), status, content);
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
        Request request = client.newRequest(baseUrl);

        if (MDC.get(REQUEST_ID_HEADER) != null) {
            request = request.header(REQUEST_ID_HEADER, MDC.get(REQUEST_ID_HEADER));
        }

        authenticationStrategy.setAuthenticationHeaders(request);

        return request;
    }
}
