package no.ks.fiks.dokumentlager.klient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class Sokeresultat {

    private final Integer totaltAntallTreff;
    private final List<Soketreff> dokumenter;

    @JsonCreator
    public Sokeresultat(@JsonProperty("totaltAntallTreff") @NonNull Integer totaltAntallTreff, @JsonProperty("dokumenter") @NonNull List<Soketreff> dokumenter) {
        this.totaltAntallTreff = totaltAntallTreff;
        this.dokumenter = dokumenter;
    }

}
