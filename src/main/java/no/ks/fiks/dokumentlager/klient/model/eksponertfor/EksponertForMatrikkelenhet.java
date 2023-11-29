package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import lombok.Value;


@Value
public class EksponertForMatrikkelenhet implements EksponertFor {

    Matrikkelenhet matrikkelenhet;

    @Override
    public EksponertForType getType() {
        return EksponertForType.MATRIKKELENHET;
    }

    public record Matrikkelenhet(String kommunenummer, String gardsnummer, String bruksnummer, String festenummer, String seksjonsnummer) {}
}