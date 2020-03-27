package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;

@Value
public class EksponertForPerson implements EksponertFor {

    private final String fnr;

    @Override
    public EksponertForType getType() {
        return EksponertForType.PERSON;
    }

    public String toString() {
        return "EksponertForPerson(fnr=***********)";
    }
}
