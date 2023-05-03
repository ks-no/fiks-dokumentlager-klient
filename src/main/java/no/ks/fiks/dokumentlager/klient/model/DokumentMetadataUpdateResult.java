package no.ks.fiks.dokumentlager.klient.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DokumentMetadataUpdateResult {

    private final UUID id;
    private final OffsetDateTime tilgjengeligTil;

    @JsonCreator
    public DokumentMetadataUpdateResult(@JsonProperty("id") UUID id,
                                        @JsonProperty("tilgjengeligTil") OffsetDateTime tilgjengeligTil) {
        this.id = id;
        this.tilgjengeligTil = tilgjengeligTil;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getTilgjengeligTil() {
        return tilgjengeligTil;
    }

}
