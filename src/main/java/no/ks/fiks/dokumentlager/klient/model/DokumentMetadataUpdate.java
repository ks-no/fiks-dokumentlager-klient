package no.ks.fiks.dokumentlager.klient.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DokumentMetadataUpdate {
    private Long ttl;
    private OffsetDateTime tilgjengeligTil;
}
