package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EksponertForPerson.class, name = "PERSON"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANSASJON_FULLMAKT"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON_BYGGESAKER"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON_FAKTURA"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON_POST"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON_SKJEMA"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON_ANDRESAKER"),
        @JsonSubTypes.Type(value = EksponertForIntegrasjon.class, name = "INTEGRASJON"),
        @JsonSubTypes.Type(value = EksponertForAutorisasjon.class, name = "AUTORISASJON"),
        @JsonSubTypes.Type(value = EksponertForMatrikkelenhet.class, name = "MATRIKKELENHET")
})
public interface EksponertFor {
    EksponertForType getType();
}