package no.ks.fiks.dokumentlager.klient.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import no.ks.fiks.dokumentlager.klient.model.eksponertfor.EksponertFor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Data
@Builder
public class DokumentMetadataUpload {
    private String dokumentnavn;
    private String mimetype;
    private Long ttl;
    private Set<EksponertFor> eksponertFor;
    private Integer sikkerhetsniva;
    private String korrelasjonsid;
}
