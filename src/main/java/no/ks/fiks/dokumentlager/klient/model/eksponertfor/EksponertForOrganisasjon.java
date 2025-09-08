package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForOrganisasjon implements EksponertFor {

    private final String orgnr;
    private final EksponertForOrgType eksponertForOrgType;

    public EksponertForOrganisasjon(String orgnr, EksponertForOrgType eksponertForOrgType) {
        this.orgnr = orgnr;
        this.eksponertForOrgType = eksponertForOrgType;
    }

    public EksponertForOrganisasjon(String orgnr) {
        this(orgnr, EksponertForOrgType.ORGANISASJON);
    }

    @Override
    public EksponertForType getType() {
        return EksponertForType.ORGANISASJON;
    }
}