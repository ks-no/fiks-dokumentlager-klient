package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

import java.util.UUID;

@Value
public class EksponertForAutorisasjon implements EksponertFor {

    private final String privilegium;
    private final UUID ressurs;

    @Override
    public EksponertForType getType() {
        return EksponertForType.AUTORISASJON;
    }
}
