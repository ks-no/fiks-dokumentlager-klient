package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForOrganisasjon implements EksponertFor {

    String orgnr;
    RessursType ressursType;

    public EksponertForOrganisasjon(String orgnr, RessursType ressursType) {
        this.orgnr = orgnr;
        this.ressursType = ressursType;
    }

    public EksponertForOrganisasjon(String orgnr) {
        this(orgnr, null);
    }

    @Override
    public EksponertForType getType() {
        return EksponertForType.ORGANISASJON;
    }
}