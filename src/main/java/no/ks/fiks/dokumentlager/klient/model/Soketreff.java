package no.ks.fiks.dokumentlager.klient.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@JsonDeserialize()
public class Soketreff {
    UUID id;
    String dokumentnavn;
    Long kryptertStorrelse;
    Long ukryptertStorrelse;
    Boolean lest;
    OffsetDateTime opprettet;
    String mimetype;
    Boolean slettet;
    UUID korrelasjonsid;
}
