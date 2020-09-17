package no.ks.fiks.dokumentlager.klient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
public class Soketreff {

    private final UUID id;
    private final String dokumentnavn;
    private final Long kryptertStorrelse;
    private final Long ukryptertStorrelse;
    private final Boolean lest;
    private final OffsetDateTime opprettet;
    private final String mimetype;
    private final Boolean slettet;
    private final String korrelasjonsid;

    @JsonCreator
    public Soketreff(
            @JsonProperty("id") @NonNull UUID id,
            @JsonProperty("dokumentnavn") @NonNull String dokumentnavn,
            @JsonProperty("kryptertStorrelse") @NonNull Long kryptertStorrelse,
            @JsonProperty("ukryptertStorrelse") @NonNull Long ukryptertStorrelse,
            @JsonProperty("lest") @NonNull Boolean lest,
            @JsonProperty("opprettet") @NonNull OffsetDateTime opprettet,
            @JsonProperty("mimetype") @NonNull String mimetype,
            @JsonProperty("slettet") @NonNull Boolean slettet,
            @JsonProperty("korrelasjonsid") @NonNull String korrelasjonsid) {
        this.id = id;
        this.dokumentnavn = dokumentnavn;
        this.kryptertStorrelse = kryptertStorrelse;
        this.ukryptertStorrelse = ukryptertStorrelse;
        this.lest = lest;
        this.opprettet = opprettet;
        this.mimetype = mimetype;
        this.slettet = slettet;
        this.korrelasjonsid = korrelasjonsid;
    }
}
