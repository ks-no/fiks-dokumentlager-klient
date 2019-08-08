package no.ks.fiks.dokumentlager.klient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class DokumentMetadataDownloadResult {

    private final UUID id;
    private final String dokumentnavn;
    private final String mimeType;
    private final Long kryptertStorrelse;
    private final Long ukryptertStorrelse;

    @JsonCreator
    public DokumentMetadataDownloadResult(@JsonProperty("id") @NonNull UUID id, @JsonProperty("dokumentnavn") @NonNull String dokumentnavn, @JsonProperty("mimeType") @NonNull String mimeType, @JsonProperty("kryptertStorrelse") @NonNull Long kryptertStorrelse, @JsonProperty("ukryptertStorrelse") @NonNull Long ukryptertStorrelse) {
        this.id = id;
        this.dokumentnavn = dokumentnavn;
        this.mimeType = mimeType;
        this.kryptertStorrelse = kryptertStorrelse;
        this.ukryptertStorrelse = ukryptertStorrelse;
    }

}
