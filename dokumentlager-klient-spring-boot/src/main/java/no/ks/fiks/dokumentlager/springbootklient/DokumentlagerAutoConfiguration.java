package no.ks.fiks.dokumentlager.springbootklient;

import no.ks.fiks.dokumentlager.klient.DokumentlagerApi;
import no.ks.fiks.dokumentlager.klient.DokumentlagerApiImpl;
import no.ks.fiks.dokumentlager.klient.DokumentlagerKlient;
import no.ks.fiks.dokumentlager.klient.authentication.IntegrasjonAuthenticationStrategy;
import no.ks.fiks.maskinporten.Maskinportenklient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DokumentlagerIntegrasjonConfiguration.class, DokumentlagerUploadServiceHost.class, DokumentlagerDownloadServiceHost.class})
public class DokumentlagerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(DokumentlagerKlient.class)
  public DokumentlagerKlient dokumentlagerKlient(Maskinportenklient maskinportenklient,
                                                 DokumentlagerIntegrasjonConfiguration integrasjonConfiguration,
                                                 DokumentlagerUploadServiceHost dokumentlagerUploadServiceHost,
                                                 DokumentlagerDownloadServiceHost dokumentlagerDownloadServiceHost) {
    DokumentlagerApi api = new DokumentlagerApiImpl(
            dokumentlagerUploadServiceHost.getUrl(),
            dokumentlagerDownloadServiceHost.getUrl(),
            new IntegrasjonAuthenticationStrategy(maskinportenklient,
                    integrasjonConfiguration.getId(),
                    integrasjonConfiguration.getPassord()));
    return DokumentlagerKlient.builder().api(api).build();
  }
}