package no.ks.fiks.dokumentlager.klient;

import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataDownloadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import no.ks.fiks.dokumentlager.klient.model.Sokeresultat;
import no.ks.fiks.dokumentlager.klient.model.eksponertfor.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JsonMapperTest {

    @RepeatedTest(100)
    @DisplayName("Test at metadata serialiseres korrekt")
    void testToJson() {
        DokumentMetadataUpload metadata = DokumentMetadataUpload.builder()
                .dokumentnavn(UUID.randomUUID().toString())
                .mimetype(UUID.randomUUID().toString())
                .ttl(ThreadLocalRandom.current().nextLong())
                .tilgjengeligTil(OffsetDateTime.now())
                .eksponertFor(randomEksponertForSet())
                .sikkerhetsniva(ThreadLocalRandom.current().nextInt())
                .korrelasjonsid(UUID.randomUUID())
                .ikkeLagreDokumentLastetNed(ThreadLocalRandom.current().nextBoolean())
                .build();
        String json = new JsonMapper()
                .toJson(metadata);
        assertHasJsonFieldWithValue(json, "dokumentnavn", metadata.getDokumentnavn());
        assertHasJsonFieldWithValue(json, "mimetype", metadata.getMimetype());
        assertHasJsonFieldWithValue(json, "ttl", metadata.getTtl());
        assertHasJsonFieldWithValue(json, "tilgjengeligTil", metadata.getTilgjengeligTil().format(DateTimeFormatter.ISO_DATE_TIME));
        metadata.getEksponertFor().forEach(eksponertFor -> assertHasEksponertFor(json, eksponertFor));
        assertHasJsonFieldWithValue(json, "sikkerhetsniva", metadata.getSikkerhetsniva());
        assertHasJsonFieldWithValue(json, "korrelasjonsid", metadata.getKorrelasjonsid().toString());
        assertHasJsonFieldWithValue(json, "ikkeLagreDokumentLastetNed", metadata.getIkkeLagreDokumentLastetNed());
    }

    private void assertHasJsonFieldWithValue(String json, String field, Object value) {
        if (value instanceof String) {
            assertThat(json, containsString(String.format("\"%s\":\"%s\"", field, value)));
        } else {
            assertThat(json, containsString(String.format("\"%s\":%s", field, value)));
        }
    }

    private void assertHasEksponertFor(String json, EksponertFor eksponertFor) {
        if (eksponertFor instanceof EksponertForPerson) {
            assertThat(json, containsString(String.format("{\"fnr\":\"%s\",\"type\":\"PERSON\"}", ((EksponertForPerson) eksponertFor).getFnr())));
        } else if (eksponertFor instanceof EksponertForOrganisasjon) {
            assertThat(json, containsString(String.format("{\"orgnr\":\"%s\",\"eksponertForOrgType\":\"ORGANISASJON\",\"type\":\"ORGANISASJON\"}", ((EksponertForOrganisasjon) eksponertFor).getOrgnr())));
        } else if (eksponertFor instanceof EksponertForIntegrasjon) {
            assertThat(json, containsString(String.format("{\"id\":\"%s\",\"type\":\"INTEGRASJON\"}", ((EksponertForIntegrasjon) eksponertFor).getId())));
        } else if (eksponertFor instanceof EksponertForAutorisasjon) {
            assertThat(json, containsString(String.format("{\"privilegium\":\"%s\",\"ressurs\":\"%s\",\"type\":\"AUTORISASJON\"}", ((EksponertForAutorisasjon) eksponertFor).getPrivilegium(), ((EksponertForAutorisasjon) eksponertFor).getRessurs())));
        }
    }

    private Set<EksponertFor> randomEksponertForSet() {
        Set<EksponertFor> eksponertFor = new HashSet<>();
        for (int i = 0; i < ThreadLocalRandom.current().nextInt(1, 10); i++) {
            eksponertFor.add(randomEksponertFor());
        }
        return eksponertFor;
    }

    private static EksponertFor randomEksponertFor() {
        return switch (ThreadLocalRandom.current().nextInt(0, 4)) {
            case 0 -> new EksponertForPerson(UUID.randomUUID().toString());
            case 1 -> new EksponertForOrganisasjon(UUID.randomUUID().toString(), EksponertForOrgType.ORGANISASJON);
            case 2 -> new EksponertForIntegrasjon(UUID.randomUUID());
            case 3 -> new EksponertForAutorisasjon(UUID.randomUUID().toString(), UUID.randomUUID());
            default -> throw new RuntimeException("Ugyldig index for eksponertFor");
        };
    }

    @RepeatedTest(100)
    @DisplayName("Test at upload result deserialiseres korrekt")
    void testFromJsonUpload() {
        UUID id = UUID.randomUUID();
        String dokumentnavn = UUID.randomUUID().toString();
        String mimeType = UUID.randomUUID().toString();
        long ukryptertStorrelse = ThreadLocalRandom.current().nextLong();
        long kryptertStorrelse = ThreadLocalRandom.current().nextLong();

        DokumentMetadataUploadResult result = new JsonMapper()
                .fromJson(
                        String.format(
                                "{\"id\":\"%s\",\"dokumentnavn\":\"%s\",\"mimeType\":\"%s\",\"ukryptertStorrelse\":%d,\"kryptertStorrelse\":%d}",
                                id, dokumentnavn, mimeType, ukryptertStorrelse, kryptertStorrelse).getBytes(),
                        DokumentMetadataUploadResult.class);

        assertThat(result.getId(), is(id));
        assertThat(result.getDokumentnavn(), is(dokumentnavn));
        assertThat(result.getMimeType(), is(mimeType));
        assertThat(result.getUkryptertStorrelse(), is(ukryptertStorrelse));
        assertThat(result.getKryptertStorrelse(), is(kryptertStorrelse));
    }

    @RepeatedTest(100)
    @DisplayName("Test at download result deserialiseres korrekt")
    void testFromJsonDownload() {
        UUID id = UUID.randomUUID();
        String dokumentnavn = UUID.randomUUID().toString();
        String mimeType = UUID.randomUUID().toString();
        long kryptertStorrelse = ThreadLocalRandom.current().nextLong();
        long ukryptertStorrelse = ThreadLocalRandom.current().nextLong();
        UUID korrelasjonsid = UUID.randomUUID();

        DokumentMetadataDownloadResult result = new JsonMapper()
                .fromJson(
                        String.format(
                                "{\"id\":\"%s\",\"dokumentnavn\":\"%s\",\"mimeType\":\"%s\",\"ukryptertStorrelse\":%d,\"kryptertStorrelse\":%d,\"korrelasjonsid\":\"%s\"}",
                                id, dokumentnavn, mimeType, ukryptertStorrelse, kryptertStorrelse, korrelasjonsid).getBytes(),
                        DokumentMetadataDownloadResult.class);

        assertThat(result.getId(), is(id));
        assertThat(result.getDokumentnavn(), is(dokumentnavn));
        assertThat(result.getMimeType(), is(mimeType));
        assertThat(result.getUkryptertStorrelse(), is(ukryptertStorrelse));
        assertThat(result.getKryptertStorrelse(), is(kryptertStorrelse));
        assertThat(result.getKorrelasjonsid(), is(korrelasjonsid));
    }

    @RepeatedTest(100)
    @DisplayName("Test at download søkeresultat deserialiseres korrekt")
    void testFromJsonSokeresultat() {
        int totaltAntallTreff = ThreadLocalRandom.current().nextInt();
        UUID id = UUID.randomUUID();
        String dokumentnavn = UUID.randomUUID().toString();
        long kryptertStorrelse = ThreadLocalRandom.current().nextLong();
        long ukryptertStorrelse = ThreadLocalRandom.current().nextLong();
        OffsetDateTime opprettet = OffsetDateTime.now();
        String mimetype = UUID.randomUUID().toString();
        boolean slettet = ThreadLocalRandom.current().nextBoolean();
        UUID korrelasjonsid = UUID.randomUUID();

        Sokeresultat result = new JsonMapper()
                .fromJson(
                        String.format(
                                "{\"totaltAntallTreff\":%d,\"dokumenter\":[{\"id\":\"%s\",\"dokumentnavn\":\"%s\",\"kryptertStorrelse\":%d,\"ukryptertStorrelse\":%d,\"opprettet\":\"%s\",\"mimetype\":\"%s\",\"slettet\":%b,\"korrelasjonsid\":\"%s\"}]}",
                                totaltAntallTreff, id, dokumentnavn, kryptertStorrelse, ukryptertStorrelse, opprettet, mimetype, slettet, korrelasjonsid).getBytes(),
                        Sokeresultat.class);

        assertThat(result.getTotaltAntallTreff(), is(totaltAntallTreff));
        assertThat(result.getDokumenter(), hasSize(1));
        assertThat(result.getDokumenter().get(0).getId(), is(id));
        assertThat(result.getDokumenter().get(0).getDokumentnavn(), is(dokumentnavn));
        assertThat(result.getDokumenter().get(0).getKryptertStorrelse(), is(kryptertStorrelse));
        assertThat(result.getDokumenter().get(0).getUkryptertStorrelse(), is(ukryptertStorrelse));
        assertThat(result.getDokumenter().get(0).getOpprettet(), is(opprettet));
        assertThat(result.getDokumenter().get(0).getMimetype(), is(mimetype));
        assertThat(result.getDokumenter().get(0).getSlettet(), is(slettet));
        assertThat(result.getDokumenter().get(0).getKorrelasjonsid(), is(korrelasjonsid));
    }

    @Test
    @DisplayName("Test at deserialisering ignorerer ukjente felter")
    void testFromJsonIgnoreUnknown() {
        UUID id = UUID.randomUUID();
        String navn = UUID.randomUUID().toString();
        TestJson result = new JsonMapper()
                .fromJson(
                        String.format("{\"id\":\"%s\",\"ukjent\":\"%s\",\"navn\":\"%s\"}", id, UUID.randomUUID(), navn).getBytes(),
                        TestJson.class);

        assertThat(result.id, is(id));
        assertThat(result.navn, is(navn));
    }

    private record TestJson(UUID id, String navn) {}
}
