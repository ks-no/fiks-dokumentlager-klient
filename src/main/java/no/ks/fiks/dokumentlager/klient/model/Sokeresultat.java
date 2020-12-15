package no.ks.fiks.dokumentlager.klient.model;

import lombok.Value;

import java.util.List;

@Value
public class Sokeresultat {
    Integer totaltAntallTreff;
    List<Soketreff> dokumenter;
}
