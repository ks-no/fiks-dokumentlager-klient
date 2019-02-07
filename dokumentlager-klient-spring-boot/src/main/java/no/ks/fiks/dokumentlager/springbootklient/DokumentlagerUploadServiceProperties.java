package no.ks.fiks.dokumentlager.springbootklient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix="fiks-dokumentlager-upload-service")
public class DokumentlagerUploadServiceProperties {
    @NotBlank private String scheme;
    @NotBlank private String host;
    @NotNull private Integer port;

    public String getUrl() {
        return String.format("%s://%s:%s", getScheme(), getHost(), getPort());
    }
}
