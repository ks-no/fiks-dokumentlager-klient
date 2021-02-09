package no.ks.fiks.dokumentlager.klient.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import no.ks.fiks.dokumentlager.klient.model.eksponertfor.EksponertFor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class DokumentMetadataUpload {
    @ToString.Exclude
    private String dokumentnavn;
    private String mimetype;
    private Long ttl;
    private Set<EksponertFor> eksponertFor;
    private Integer sikkerhetsniva;
    private UUID korrelasjonsid;
    private Boolean lagreDokumentLastetNed;
}
