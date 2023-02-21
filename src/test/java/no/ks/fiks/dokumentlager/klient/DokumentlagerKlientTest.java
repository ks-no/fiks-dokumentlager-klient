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
import static no.ks.fiks.dokumentlager.klient.EncryptUtil.PRIVATE_KEY;
import static no.ks.fiks.dokumentlager.klient.EncryptUtil.PUBLIC_KEY;
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

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
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

        assertThrows(RuntimeException.class, () ->
                klient.upload(new ByteArrayInputStream(data), metadata, fiksOrganisasjonId, kontoId, true));
        assertThrows(RuntimeException.class, () ->
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
        String message = UUID.randomUUID().toString();
        Exception expected = new IllegalArgumentException(message);
        CMSStreamKryptering kryptering = mock(CMSStreamKryptering.class);
        doThrow(expected).when(kryptering).krypterData(any(OutputStream.class), any(InputStream.class), any(X509Certificate.class), any(Provider.class));

        klient = DokumentlagerKlient.builder()
                .api(api)
                .kryptering(kryptering)
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                klient.upload(new ByteArrayInputStream(new byte[0]), DokumentMetadataUpload.builder().build(), UUID.randomUUID(), UUID.randomUUID(), true));

        assertThat(exception.getMessage(), is(message));
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
