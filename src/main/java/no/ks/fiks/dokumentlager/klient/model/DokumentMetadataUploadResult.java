package no.ks.fiks.dokumentlager.klient.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DokumentMetadataUploadResult {
    private final UUID id;

    @JsonCreator
    public DokumentMetadataUploadResult(@JsonProperty("id") UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
