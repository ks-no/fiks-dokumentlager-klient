package no.ks.fiks.dokumentlager.klient;

import no.ks.fiks.dokumentlager.klient.model.*;
import no.ks.fiks.dokumentlager.klient.model.eksponertfor.EksponertForIntegrasjon;
import no.ks.kryptering.CMSKrypteringImpl;
import no.ks.kryptering.CMSStreamKryptering;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NoRouteToHostException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DokumentlagerKlientTest {

    private DokumentlagerKlient klient;
    private DokumentlagerApi api;

    private final CMSKrypteringImpl kryptering = new CMSKrypteringImpl();
    private final Provider provider = Security.getProvider("BC");
    private final PrivateKey privateKey = getPrivateKey();

    private byte[] uploadedBytes;

    private static final String PUBLIC_KEY =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEIDCCAwigAwIBAgIJAOfdsbcJ9VCaMA0GCSqGSIb3DQEBCwUAMIGjMQswCQYD\n" +
            "VQQGEwJOTzEWMBQGA1UECAwNRG9rdW1lbnRsYWdlcjEWMBQGA1UEBwwNRG9rdW1l\n" +
            "bnRsYWdlcjEWMBQGA1UECgwNRG9rdW1lbnRsYWdlcjEWMBQGA1UECwwNRG9rdW1l\n" +
            "bnRsYWdlcjEWMBQGA1UEAwwNRG9rdW1lbnRsYWdlcjEcMBoGCSqGSIb3DQEJARYN\n" +
            "RG9rdW1lbnRsYWdlcjAgFw0xOTAxMjIwNzExNDlaGA80NzU2MTIxODA3MTE0OVow\n" +
            "gaMxCzAJBgNVBAYTAk5PMRYwFAYDVQQIDA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQH\n" +
            "DA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQKDA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQL\n" +
            "DA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQDDA1Eb2t1bWVudGxhZ2VyMRwwGgYJKoZI\n" +
            "hvcNAQkBFg1Eb2t1bWVudGxhZ2VyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n" +
            "CgKCAQEAozKOfBKRoU7AgRAfvROwAKGPuZOyp5x7WB0pZZca7xhB01k0CZGsPr42\n" +
            "6B1MtufuDlbMEBnUsbuPGrU5jZ02OOcITXa9t8t4GF0UnwffYa9Jn1GewTYzP0oo\n" +
            "rNCXMJyzsZOVUSOvctG/X5z8i5TZs9gtSYun0rvBqENKGJZubx67aTtABfAuDioY\n" +
            "xsW0KBt2LuhrcykoH9hJYdPBvS8PuCAIzhXxWG/VEHAnS+x4jpR7UkKt3yGtRa8s\n" +
            "OZ94xosXjNj6vAtb1TpvcfZV/9E2bxJtUVIPaAS2jt2Qo0pc6ea05MSSxsl574am\n" +
            "J/F9nQ9FMs6t9ZIeBdU95abu8rOxOwIDAQABo1MwUTAdBgNVHQ4EFgQUHnsFG7Kx\n" +
            "IOwibkHTnBwY79jbjKkwHwYDVR0jBBgwFoAUHnsFG7KxIOwibkHTnBwY79jbjKkw\n" +
            "DwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAGBJOnx1zOZYbwqxG\n" +
            "iGbR98ms2OydjUBbaiB9SneWomTXGXSI3j/7xUlCyQFLQiivUI2Ip5x0nhPMOaYK\n" +
            "yTYy6rZ/geBmcOpWihd4LnNLO3AT2dYcptdG213yIomjRk4BNaAQCMt9ZcicTzxG\n" +
            "eV1oa2ERdRt+y8fkVd0QZ6lhXOssW2vMt9AC2k4LL9woJrgZs4CvtCDKHET+HvvI\n" +
            "7NbuaTZSNolZwR5hIdtq8nKCvNVp4VvOFgT8WIuudMx1tWgDIo9ttLCV7tz9WjtL\n" +
            "L9fbdxYO5UGayVq0IFt4gCQLpkcaThaqRVeC+l7PU7WHHqrUzsnSQpm8Hp40D2zI\n" +
            "dkMkSA==\n" +
            "-----END CERTIFICATE-----";

    private static final String PRIVATE_KEY =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCjMo58EpGhTsCB\n" +
            "EB+9E7AAoY+5k7KnnHtYHSlllxrvGEHTWTQJkaw+vjboHUy25+4OVswQGdSxu48a\n" +
            "tTmNnTY45whNdr23y3gYXRSfB99hr0mfUZ7BNjM/Siis0JcwnLOxk5VRI69y0b9f\n" +
            "nPyLlNmz2C1Ji6fSu8GoQ0oYlm5vHrtpO0AF8C4OKhjGxbQoG3Yu6GtzKSgf2Elh\n" +
            "08G9Lw+4IAjOFfFYb9UQcCdL7HiOlHtSQq3fIa1Fryw5n3jGixeM2Pq8C1vVOm9x\n" +
            "9lX/0TZvEm1RUg9oBLaO3ZCjSlzp5rTkxJLGyXnvhqYn8X2dD0Uyzq31kh4F1T3l\n" +
            "pu7ys7E7AgMBAAECggEALKBJiDIHsqV3TJOdKjX0/ecwBx4VT3Ih5HFs/YO5cMIg\n" +
            "Vevhp/A2up2HJCfG74kydqdTe9+kYsmYE0SVLV1dE2hRw+UBcf3opDjnx6j+c5bc\n" +
            "Of22vLzWfKsJvl/3x+pB1QA3Z42rj2k9vKaQBJc6hMxLbf4LcTu4dAuaemjAYBAG\n" +
            "gXoBeG5m7APYEaGIbGdl8UEWMf/GrarDMAYMWoatKIRkhwYVazfjaoVxk2kybCA0\n" +
            "RjsGjrXTETojKmFi2ImQYC9VVdOSGBlqTfuJHv3MaWOD09W3/IfRrMWag0UrX1zB\n" +
            "T4IXnZFl+1AHVJ6AXC6114mW1hFJuxGnAlXbEHUJOQKBgQDPN0gvU5l/MD1UzL8Y\n" +
            "kyiJtE6fjXo2Z0mcwXv/ZocKq/BvuEjbav4QzmF/6oiC9zpgmbvxsZpjyz8vkqc6\n" +
            "vDTmE07Bkp+bxXXI821KONLCsIyRxDXT7JSyRWCoD0TAEX/IkYh6GWAxqtnzaTQi\n" +
            "gqelHI/oId+fIp6K9UHBGAW7JQKBgQDJnlJjpJWEcvLOjhST3m7eeGotrR2SO/U9\n" +
            "+nouLnGglDpEp8UbvLbTPLAoYBRaFLxr95quCh/+96f2wmrcfWGt+K4K3LKtDZbu\n" +
            "0Clg4RuhZWokTJ7QNpKgTGmQCsboMM9AJTBPQCj2uESb7+V/LVTPzyvs9U7RgaRn\n" +
            "8YLqI6U83wKBgBWbnCljPFRpAVxAZYT4g3eol7JHnIDj0GdKPdXqKRbRyya7Ps2y\n" +
            "oH+8JaqjGE0f3rSIE3MmpATYAuTBFDMpwRJk3QeOdJpXwuqLh8//kOrAYkgo/7vz\n" +
            "paXZWjTsMq0cpgiSNHsW/lLvj/6z773Rhg3Ppqn8Lkd34rR20r6B9McJAoGAH+JF\n" +
            "rTRN4NA8zaVyY5/9cHkicW67CnEo61A9GiiGF5rZTBor9aL2Vpl2Uiw/i69TzM8v\n" +
            "Su6W+L85dLByLcQ2OkjlXRphtzQ69jE9GfD/aZqcGnlzdAHtViQ/XWQW6IkvfTlk\n" +
            "VmQTFlE1qGNbq60DiIl+rM5uVHtoAHgU9+oDK4kCgYBwAD1SPW7b3OKoleyjUew6\n" +
            "FoJ952ShhEeYmyMpwhYayUY1SEdcjJlxDj5RyVCvWq2xu7LL/7uLRaQR9XiLLwxE\n" +
            "m+q8ASYbc1Fh8UykUNFcOKqTImWA0CGuDndc1ZXxwA9SN9/UNap21dfZrJN/VhBN\n" +
            "Ad1DLPxU/e3rN6lr/Yopqw==\n";

    private PrivateKey getPrivateKey() {
        try {
            Base64.Decoder base64 = Base64.getMimeDecoder();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(base64.decode(PRIVATE_KEY));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void beforeEach() {
        api = mock(DokumentlagerApi.class);
        when(api.getPublicKey()).thenReturn(DokumentlagerResponse.<String>builder()
                .result(PUBLIC_KEY)
                .httpStatus(200)
                .build());
        when(api.uploadDokument(any(InputStream.class), any(DokumentMetadataUpload.class), any(UUID.class), any(UUID.class), anyBoolean()))
                .then(a -> {
                    try (InputStream inputStream = a.getArgument(0)) {
                        DokumentMetadataUpload metadata = a.getArgument(1);
                        uploadedBytes = IOUtils.toByteArray(inputStream);
                        return DokumentlagerResponse.<DokumentMetadataUploadResult>builder()
                                .result(new DokumentMetadataUploadResult(UUID.randomUUID(), metadata.getDokumentnavn(), metadata.getMimetype(), (long) uploadedBytes.length, (long) uploadedBytes.length + 500))
                                .httpStatus(200)
                                .httpHeaders(emptyMap())
                                .build();
                    }
                });
        klient = DokumentlagerKlient.builder()
                .api(api)
                .build();
    }

    @AfterEach
    void afterEach() {
        uploadedBytes = null;
        api = null;
        klient = null;
    }

    @Test
    @DisplayName("Ved opplasting av et dokument med sikkerhetsnivå 3 og uten kryptert-flagg skal API kalles med innsendt data")
    void uploadDokumentNiva3UtenFlag() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);

        ByteArrayInputStream dokumentData = new ByteArrayInputStream(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentNiva3UtenFlag.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        klient.upload(dokumentData, metadata, fiksOrganisasjonId, kontoId);
        verify(api, times(1)).uploadDokument(eq(dokumentData), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(false));
    }

    @Test
    @DisplayName("Ved opplasting av et dokument som allerede er kryptert skal API kalles med innsendt data")
    void uploadAlreadyEncryptedDokument() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);

        ByteArrayInputStream dokumentData = new ByteArrayInputStream(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadAlreadyEncryptedDokument.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        klient.uploadAlreadyEncrypted(dokumentData, metadata, fiksOrganisasjonId, kontoId);
        verify(api, times(1)).uploadDokument(eq(dokumentData), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(true));
    }

    @Test
    @DisplayName("Ved opplasting av et dokument med sikkerhetsnivå 4 og uten kryptert-flagg skal API kalles med kryptert data")
    void uploadDokumentNiva4UtenFlag() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);

        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentNiva4UtenFlag.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(4)
                .build();

        klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId);
        verify(api, times(1)).uploadDokument(any(InputStream.class), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(true));
        assertDataEncrypted(data);
    }

    @Test
    @DisplayName("Ved opplasting av et dokument med kryptert-flagg satt skal klienten kryptere dokumentet før opplasting")
    void uploadDokumentKryptert() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);

        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentKryptert.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true);
        verify(api, times(1)).uploadDokument(any(InputStream.class), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(true));
        assertDataEncrypted(data);
    }

    @Test
    @DisplayName("Ved opplasting av et dokument hvis APIet er nede skal riktig exception kastes")
    void uploadDokumentApiError() {
        Exception expected = new NoRouteToHostException("No route to host");
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentKryptert.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        DokumentlagerApi api = mock(DokumentlagerApi.class);
        when(api.getPublicKey()).thenReturn(DokumentlagerResponse.<String>builder()
                .result(PUBLIC_KEY)
                .httpStatus(200)
                .build());
        when(api.uploadDokument(any(InputStream.class), any(DokumentMetadataUpload.class), any(UUID.class), any(UUID.class), anyBoolean()))
                .then(a -> {
                        throw new NoRouteToHostException("No route to host");
                    });
        DokumentlagerKlient klient = DokumentlagerKlient.builder()
                .api(api)
                .build();

        NoRouteToHostException exception = assertThrows(NoRouteToHostException.class, () ->
                klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true));
        assertThat(exception.getMessage(), is(expected.getMessage()));
        verify(api, times(1)).uploadDokument(any(InputStream.class), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(true));
    }

    @Test
    @DisplayName("Ved opplasting av et dokument hvis APIet er nede skal futures bli kansellert og thread pool ryddet opp i")
    void uploadDokumentApiErrorCleanThreadPool() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentKryptert.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        DokumentlagerApi api = mock(DokumentlagerApi.class);
        when(api.getPublicKey()).thenReturn(DokumentlagerResponse.<String>builder()
                .result(PUBLIC_KEY)
                .httpStatus(200)
                .build());
        when(api.uploadDokument(any(InputStream.class), any(DokumentMetadataUpload.class), any(UUID.class), any(UUID.class), anyBoolean()))
                .then(a -> {
                    throw new NoRouteToHostException("No route to host");
                })
                .thenAnswer(a -> {
                    throw new NoRouteToHostException("No route to host");
                })
                .thenAnswer(a -> {
                    try (InputStream inputStream = a.getArgument(0)) {
                    uploadedBytes = IOUtils.toByteArray(inputStream);
                    return DokumentlagerResponse.<DokumentMetadataUploadResult>builder()
                        .result(new DokumentMetadataUploadResult(UUID.randomUUID(), metadata.getDokumentnavn(), metadata.getMimetype(), (long) uploadedBytes.length, (long) uploadedBytes.length + 500))
                        .httpStatus(200)
                        .httpHeaders(emptyMap())
                        .build();
                    }
                });
        DokumentlagerKlient klient = DokumentlagerKlient.builder()
                .api(api)
                .executor(Executors.newFixedThreadPool(2))
                .build();

        assertThrows(NoRouteToHostException.class, () ->
                klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true));
        assertThrows(NoRouteToHostException.class, () ->
                klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true));
        DokumentlagerResponse<DokumentMetadataUploadResult> dokumentlagerResponse = klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true);
        assertThat(dokumentlagerResponse.getHttpStatus(), is(200));
    }

    @Test
    @DisplayName("Ved opplasting av et dokument skal korrekt metadata returneres")
    void uploadMetadata() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);
        String dokumentnavn = UUID.randomUUID().toString();
        String mimetype = UUID.randomUUID().toString();

        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn(dokumentnavn)
                .mimetype(mimetype)
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        DokumentlagerResponse<DokumentMetadataUploadResult> upload = klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, false);
        assertThat(upload.getResult(), notNullValue());
        assertThat(upload.getResult().getDokumentnavn(), is(dokumentnavn));
        assertThat(upload.getResult().getMimeType(), is(mimetype));
        assertThat(upload.getResult().getUkryptertStorrelse(), is((long) data.length));
        assertThat(upload.getResult().getKryptertStorrelse(), is((long) data.length + 500));
    }

    private void assertDataEncrypted(byte[] originalData) {
        byte[] decryptedData = kryptering.dekrypterData(uploadedBytes, privateKey, provider);
        assertArrayEquals(originalData, decryptedData);
    }

    @Test
    @DisplayName("Ved opplasting skal public key caches etter første request")
    void uploadDokumentPublicKeyCaches() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000, 100000)];
        new Random().nextBytes(data);

        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadDokumentPublicKeyCaches.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true);
        klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true);
        verify(api, times(1)).getPublicKey();
        assertDataEncrypted(data);
    }

    @Test
    @DisplayName("Ved sletting av et dokument skal API kalles med samme parametere som klienten")
    void deleteDokument() {
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();
        UUID dokumentId = UUID.randomUUID();

        klient.delete(fiksOrganisasjonId, kontoId, dokumentId);

        verify(api, times(1)).deleteDokument(fiksOrganisasjonId, kontoId, dokumentId);
    }

    @Test
    @DisplayName("Ved nedlasting av et dokument skal API kalles med samme parametere som klienten, og stream returnert av API skal returneres")
    void downloadDokument() {
        UUID dokumentId = UUID.randomUUID();
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        Map<String, String> headers = singletonMap("header", "value");

        when(api.downloadDokument(dokumentId)).thenReturn(DokumentlagerResponse.<InputStream>builder()
                .result(inputStream)
                .httpStatus(200)
                .httpHeaders(headers)
                .build());

        DokumentlagerResponse<InputStream> response = klient.download(dokumentId);

        verify(api, times(1)).downloadDokument(dokumentId);
        assertThat(response.getResult(), is(inputStream));
        assertThat(response.getHttpStatus(), is(200));
        assertThat(response.getHeader("header").get(), is("value"));
    }

    @Test
    @DisplayName("Dersom kryptering feiler skal riktig exception kastes av klienten")
    void uploadDokumentKrypteringFeiler() {
        Exception expected = new RuntimeException("Kryptering feilet");
        CMSStreamKryptering kryptering = mock(CMSStreamKryptering.class);
        doThrow(expected).when(kryptering).krypterData(any(OutputStream.class), any(InputStream.class), any(X509Certificate.class), any(Provider.class));

        klient = DokumentlagerKlient.builder()
                .api(api)
                .kryptering(kryptering)
                .build();

        IOException exception = assertThrows(IOException.class, () ->
                klient.upload(new ByteArrayInputStream(new byte[0]), DokumentMetadataUpload.builder().build(), UUID.randomUUID(), UUID.randomUUID(), true));

        assertThat(exception.getMessage(), is(expected.getMessage()));
        assertThat(exception.getCause(), is(expected));
    }

    @Test
    @DisplayName("Det skal være mulig å laste opp flere dokumenter samtidig")
    void uploadMangeDokumenterSamtidig() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000000, 100000000)];
        new Random().nextBytes(data);

        ByteArrayInputStream dokumentData = new ByteArrayInputStream(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadMangeDokumenterSamtidig.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(3)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        List<Future> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(
                    executorService.submit(() -> {
                        klient.upload(dokumentData, metadata, fiksOrganisasjonId, kontoId);
                    })
            );
        }

        results.forEach(r -> {
            try {
                r.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        verify(api, times(20)).uploadDokument(eq(dokumentData), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(false));
        executorService.shutdown();
    }

    @Test
    @DisplayName("Det skal være mulig å laste opp flere krypterte dokumenter samtidig")
    void uploadMangeKrypterteDokumenterSamtidig() {
        byte[] data = new byte[ThreadLocalRandom.current().nextInt(10000000, 100000000)];
        new Random().nextBytes(data);

        ByteArrayInputStream dokumentData = new ByteArrayInputStream(data);
        UUID fiksOrganisasjonId = UUID.randomUUID();
        UUID kontoId = UUID.randomUUID();

        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn("uploadMangeKrypterteDokumenterSamtidig.pdf")
                .mimetype("application/pdf")
                .ttl(-1L)
                .eksponertFor(new HashSet<>(singletonList((new EksponertForIntegrasjon(UUID.randomUUID())))))
                .sikkerhetsniva(4)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        List<Future> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(
                    executorService.submit(() -> {
                        klient.upload(dokumentData, metadata, fiksOrganisasjonId, kontoId);
                    })
            );
        }

        results.forEach(r -> {
            try {
                r.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        verify(api, times(20)).uploadDokument(any(), eq(metadata), eq(fiksOrganisasjonId), eq(kontoId), eq(true));
        executorService.shutdown();
    }

    @Test
    @DisplayName("Ved nedlasting av dokument-metadata skal API kalles med samme parametere som klienten, og metadata returnert av API skal returneres")
    void downloadDokumentMetadata() {
        UUID dokumentId = UUID.randomUUID();
        DokumentMetadataDownloadResult downloadResult = new DokumentMetadataDownloadResult(UUID.randomUUID(), "dokumentnavn", "application/pdf", 123L, 100L, UUID.randomUUID());

        Map<String, String> headers = singletonMap("header", "value");

        when(api.downloadDokumentMetadata(dokumentId)).thenReturn(DokumentlagerResponse.<DokumentMetadataDownloadResult>builder()
                .result(downloadResult)
                .httpStatus(200)
                .httpHeaders(headers)
                .build());

        DokumentlagerResponse<DokumentMetadataDownloadResult> response = klient.downloadMetadata(dokumentId);

        verify(api, times(1)).downloadDokumentMetadata(dokumentId);
        assertThat(response.getResult(), is(downloadResult));
        assertThat(response.getHttpStatus(), is(200));
        assertThat(response.getHeader("header").get(), is("value"));
    }

    @Test
    @DisplayName("Ved søk etter dokumenter med korrelasjonsid skal API kalles med samme parametere som klienten, og metadata returnert av API skal returneres")
    void sokDokumenterMedKorrelasjonsid() {

        UUID id = UUID.randomUUID();
        String dokumentnavn = UUID.randomUUID().toString();
        long kryptertStorrelse = 5;
        long ukryptertStorrelse = 3;
        OffsetDateTime opprettet = OffsetDateTime.now();
        String mimetype = "application/pdf";
        Boolean slettet = false;
        UUID korrelasjonsid = UUID.randomUUID();

        Sokeresultat resultat = new Sokeresultat(1, new ArrayList<>(singletonList(new Soketreff(
                id,
                dokumentnavn,
                kryptertStorrelse,
                ukryptertStorrelse,
                opprettet,
                mimetype,
                slettet,
                korrelasjonsid
        ))));


        final UUID fiksOrganisasjonId = UUID.randomUUID();
        final UUID kontoId = UUID.randomUUID();
        when(api.sokDokumenterMedKorrelasjonsid(fiksOrganisasjonId, kontoId,korrelasjonsid,0, 5)).thenReturn(DokumentlagerResponse.<Sokeresultat>builder()
                .result(resultat)
                .httpStatus(200)
                .build());

        DokumentlagerResponse<Sokeresultat> response = klient.sokDokumenterMedKorrelasjonsid(fiksOrganisasjonId, kontoId, korrelasjonsid, 0 , 5);

        verify(api, times(1)).sokDokumenterMedKorrelasjonsid(fiksOrganisasjonId, kontoId,korrelasjonsid,0, 5);
        assertThat(response.getResult(), is(resultat));
        assertThat(response.getHttpStatus(), is(200));
    }

}
