package no.ks.fiks.dokumentlager.springbootklient;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Validated
@ConfigurationProperties(prefix = "fiks-dokumentlager-integrasjon")
public class DokumentlagerIntegrasjonConfiguration {

  @NotNull private UUID id;
  @NotBlank private String passord;

  @Override
  public String toString() {
    return "DokumentlagerIntegrasjonConfiguration{" +
            "id=" + id +
            ", passord='" + passord.replaceAll(".", "*") + '\'' +
            '}';
  }
}
