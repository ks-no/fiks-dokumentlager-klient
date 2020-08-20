package no.ks.fiks.dokumentlager.klient.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DokumentMetadataUploadResult {
    private final UUID id;
    private final String dokumentnavn;
    private final String mimeType;
    private final Long ukryptertStorrelse;
    private final Long kryptertStorrelse;

    @JsonCreator
    public DokumentMetadataUploadResult(@JsonProperty("id") UUID id,
                                        @JsonProperty("dokumentnavn") String dokumentnavn,
                                        @JsonProperty("mimeType") String mimeType,
                                        @JsonProperty("ukryptertStorrelse") Long ukryptertStorrelse,
                                        @JsonProperty("kryptertStorrelse") Long kryptertStorrelse) {
        this.id = id;
        this.dokumentnavn = dokumentnavn;
        this.mimeType = mimeType;
        this.ukryptertStorrelse = ukryptertStorrelse;
        this.kryptertStorrelse = kryptertStorrelse;
    }

    public UUID getId() {
        return id;
    }

    public String getDokumentnavn() {
        return dokumentnavn;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getUkryptertStorrelse() {
        return ukryptertStorrelse;
    }

    public Long getKryptertStorrelse() {
        return kryptertStorrelse;
    }
}
