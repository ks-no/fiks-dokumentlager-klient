package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

import java.util.UUID;

@Value
public class EksponertForIntegrasjon implements EksponertFor {

    private final UUID id;

    @Override
    public EksponertForType getType() {
        return EksponertForType.INTEGRASJON;
    }
}
