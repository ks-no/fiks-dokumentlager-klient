package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForOrganisasjonByggesaker implements EksponertFor {

    private final String orgnr;

    @Override
    public EksponertForType getType() {
        return EksponertForType.ORGANISASJON_BYGGESAKER;
    }
}
