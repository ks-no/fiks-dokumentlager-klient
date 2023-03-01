package no.ks.fiks.dokumentlager.klient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static no.ks.fiks.dokumentlager.klient.EncryptUtil.PUBLIC_KEY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TimeoutTest {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private static MockServerContainer mockServer;

    @BeforeAll
    static void setup() {
        mockServer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));
        mockServer.start();

        mockDokumentlagerPublicKey();
    }

    @Test
    @DisplayName("Upload kryptert - Test at exception kastes ved idle timeout")
    void testKryptertIdleTimeout() {
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        mockDokumentlagerUpload(fiksOrganisasjonId, kontoId);

        DokumentlagerKlient dokumentlagerKlient = DokumentlagerKlient.builder()
                .api(DokumentlagerApiImpl.builder()
                        .authenticationStrategy(request -> {})
                        .uploadBaseUrl(getMockServerHost())
                        .downloadBaseUrl(getMockServerHost())
                        .httpConfiguration(HttpConfiguration.builder()
                                .idleTimeout(Duration.ofMillis(500))
                                .uploadTimeout(Duration.ofMinutes(1))
                                .build())
                        .build())
                .build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dokumentlagerKlient.upload(
                new ByteArrayInputStream(new byte[10]),
                DokumentMetadataUpload.builder().build(),
                fiksOrganisasjonId,
                kontoId,
                true));
        assertThat(exception.getCause(), is(instanceOf(TimeoutException.class)));
        assertThat(exception.getCause().getMessage(), is("Idle timeout 500 ms"));
    }

    @Test
    @DisplayName("Upload ukryptert - Test at exception kastes ved idle timeout")
    void testUkryptertIdleTimeout() {
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        mockDokumentlagerUpload(fiksOrganisasjonId, kontoId);

        DokumentlagerKlient dokumentlagerKlient = DokumentlagerKlient.builder()
                .api(DokumentlagerApiImpl.builder()
                        .authenticationStrategy(request -> {})
                        .uploadBaseUrl(getMockServerHost())
                        .downloadBaseUrl(getMockServerHost())
                        .httpConfiguration(HttpConfiguration.builder()
                                .idleTimeout(Duration.ofMillis(1234))
                                .uploadTimeout(Duration.ofMinutes(1))
                                .build())
                        .build())
                .build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dokumentlagerKlient.upload(
                new ByteArrayInputStream(new byte[10]),
                DokumentMetadataUpload.builder().build(),
                fiksOrganisasjonId,
                kontoId));
        assertThat(exception.getCause(), is(instanceOf(TimeoutException.class)));
        assertThat(exception.getCause().getMessage(), is("Idle timeout 1234 ms"));
    }

    @Test
    @DisplayName("Upload kryptert - Test at exception kastes ved upload timeout")
    void testKryptertUploadTimeout() {
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        mockDokumentlagerUpload(fiksOrganisasjonId, kontoId);

        DokumentlagerKlient dokumentlagerKlient = DokumentlagerKlient.builder()
                .api(DokumentlagerApiImpl.builder()
                        .authenticationStrategy(request -> {})
                        .uploadBaseUrl(getMockServerHost())
                        .downloadBaseUrl(getMockServerHost())
                        .httpConfiguration(HttpConfiguration.builder()
                                .idleTimeout(Duration.ofMinutes(1))
                                .uploadTimeout(Duration.ofMillis(1111))
                                .build())
                        .build())
                .build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dokumentlagerKlient.upload(
                new ByteArrayInputStream(new byte[10]),
                DokumentMetadataUpload.builder().build(),
                fiksOrganisasjonId,
                kontoId,
                true));
        assertThat(exception.getCause(), is(instanceOf(TimeoutException.class)));
        assertThat(exception.getCause().getMessage(), is("Total timeout 1111 ms elapsed"));
    }

    @Test
    @DisplayName("Upload ukryptert - Test at exception kastes ved upload timeout")
    void testUkryptertUploadTimeout() {
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        mockDokumentlagerUpload(fiksOrganisasjonId, kontoId);

        DokumentlagerKlient dokumentlagerKlient = DokumentlagerKlient.builder()
                .api(DokumentlagerApiImpl.builder()
                        .authenticationStrategy(request -> {})
                        .uploadBaseUrl(getMockServerHost())
                        .downloadBaseUrl(getMockServerHost())
                        .httpConfiguration(HttpConfiguration.builder()
                                .idleTimeout(Duration.ofMinutes(1))
                                .uploadTimeout(Duration.ofMillis(234))
                                .build())
                        .build())
                .build();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dokumentlagerKlient.upload(
                new ByteArrayInputStream(new byte[10]),
                DokumentMetadataUpload.builder().build(),
                fiksOrganisasjonId,
                kontoId));
        assertThat(exception.getCause(), is(instanceOf(TimeoutException.class)));
        assertThat(exception.getCause().getMessage(), is("Total timeout 234 ms elapsed"));
    }

    private static void mockDokumentlagerPublicKey() {
        getMockServerClient()
                .when(request()
                        .withPath("/dokumentlager/api/v1/public-key"))
                .respond(response()
                        .withBody(PUBLIC_KEY));
    }

    private static void mockDokumentlagerUpload(UUID fiksOrganisasjonId, UUID kontoId) {
        try {
            getMockServerClient()
                    .when(request()
                            .withPath(String.format("/dokumentlager/api/v1/%s/kontoer/%s/dokumenter/", fiksOrganisasjonId, kontoId)))
                    .respond(response()
                            .withDelay(TimeUnit.SECONDS, 10)
                            .withBody(MAPPER.writeValueAsString(new DokumentMetadataUploadResult(UUID.randomUUID(), UUID.randomUUID().toString(), "application/pdf", 0L, 0L))));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static MockServerClient getMockServerClient() {
        return new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    }

    private static String getMockServerHost() {
        return String.format("http://%s:%d", mockServer.getHost(), mockServer.getServerPort());
    }
}
