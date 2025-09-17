package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForOrganisasjon implements EksponertFor {

    String orgnr;
    EksponertForOrgType eksponertForOrgType;

    public EksponertForOrganisasjon(String orgnr, EksponertForOrgType eksponertForOrgType) {
        this.orgnr = orgnr;
        this.eksponertForOrgType = eksponertForOrgType;
    }

    public EksponertForOrganisasjon(String orgnr) {
        this(orgnr, null);
    }

    @Override
    public EksponertForType getType() {
        return EksponertForType.ORGANISASJON;
    }
}