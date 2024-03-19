package no.ks.fiks.dokumentlager.klient;

import no.ks.fiks.dokumentlager.klient.authentication.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataUploadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentlagerResponse;
import no.ks.fiks.dokumentlager.klient.model.eksponertfor.EksponertForIntegrasjon;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientBuilder;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


public class DokumentlagerIntegrasjonsTest {

    private static final Logger log = LoggerFactory.getLogger(DokumentlagerIntegrasjonsTest.class);

    @Test
    public void testupload() {

        try {
            String keyStoreFilename = "/home/audun/ks/fiks-dokumentlager-klient/src/test/resources/KS-virksomhetssertifikat-auth.p12";
            String alias = "ks";
            char[] keyStorePassword = "1234".toCharArray();
            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(new FileInputStream(keyStoreFilename), keyStorePassword);
            Key key = keyStore.getKey(alias, keyStorePassword);
            Maskinportenklient maskinportenklient = new MaskinportenklientBuilder()
                    .withPrivateKey((PrivateKey) key)
                    .withProperties(
                            MaskinportenklientProperties.builder()
                                    .numberOfSecondsLeftBeforeExpire(10)
                                    .issuer("77c0a0ba-d20d-424c-b5dd-f1c63da07fc4")
                                    .audience("https://test.maskinporten.no/")
                                    .tokenEndpoint("https://test.maskinporten.no/token")
                                    .build()
                    )
                    .usingVirksomhetssertifikat((X509Certificate) keyStore.getCertificate(alias)).build();

            try (DokumentlagerKlient klient = DokumentlagerKlient.builder()
                    .api(DokumentlagerApiImpl.builder()
                            .httpConfiguration(HttpConfiguration.builder().idleTimeout(Duration.ofMinutes(5)).build())
                            .uploadBaseUrl("https://api.fiks.dev.ks.no")
                            .downloadBaseUrl("https://api.fiks.dev.ks.no")
                            .authenticationStrategy(new IntegrasjonAuthenticationStrategy(maskinportenklient, UUID.fromString("046a19e3-a7b0-4110-b9f1-edc426e56a4d"), "9*7TCYxx4G-#4RgOU5his-@XqLr2Be?i-273i#zu4Wq-Hd*6YcoB36"))
                            .requestInterceptor(request -> {
                                String requestId = Optional.ofNullable(MDC.get("requestid")).orElseGet(() -> {
                                    String newRequestId = UUID.randomUUID().toString();
                                    log.debug("Generert requestId {} for dokumentlager request", newRequestId);
                                    return newRequestId;
                                });
                                return request.headers(headers -> headers.add("requestid", requestId));
                            })
                            .build())
                    .build()) {

                for (int i = 0; i < 300; i++) {
                    try {
                        log.info("Laster opp dokument");
                        DokumentlagerResponse<DokumentMetadataUploadResult> response = klient.upload(new FileInputStream("/home/audun/ks/fiks-dokumentlager-klient/src/test/resources/small.pdf"), DokumentMetadataUpload.builder().eksponertFor(Collections.singleton(new EksponertForIntegrasjon(UUID.fromString("046a19e3-a7b0-4110-b9f1-edc426e56a4d")))).sikkerhetsniva(3).dokumentnavn("small.pdf").ttl(10000L).mimetype("application/pdf").build(), UUID.fromString("6cb106e6-6b46-41a7-8344-607d40e916ae"), UUID.fromString("f9e5de11-397a-4f09-b59d-5f3215e6830d"));
                        log.info("DokumentlagerId {}", response.getResult().getId());

                        Thread.sleep(300);
                    } catch (Exception e) {
                        log.warn("Klarte ikke å laste opp dokument", e);
                    }
                }

            }
        } catch (Exception e) {
            log.warn("Ukjent feil",e);
        }
    }
}
