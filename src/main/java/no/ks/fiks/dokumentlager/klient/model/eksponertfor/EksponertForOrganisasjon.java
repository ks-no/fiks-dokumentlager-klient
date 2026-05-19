package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForOrganisasjon implements EksponertFor {

    String orgnr;
    String ressursId;

    public EksponertForOrganisasjon(String orgnr, String ressursId) {
        this.orgnr = orgnr;
        this.ressursId = ressursId;
    }

    public EksponertForOrganisasjon(String orgnr) {
        this(orgnr, null);
    }

    @Override
    public EksponertForType getType() {
        return EksponertForType.ORGANISASJON;
    }
}