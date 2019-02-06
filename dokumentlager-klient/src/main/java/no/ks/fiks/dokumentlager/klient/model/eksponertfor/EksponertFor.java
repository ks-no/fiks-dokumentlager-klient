package no.ks.fiks.dokumentlager.klient.model.eksponertfor;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EksponertForPerson.class, name = "PERSON"),
        @JsonSubTypes.Type(value = EksponertForOrganisasjon.class, name = "ORGANISASJON"),
        @JsonSubTypes.Type(value = EksponertForIntegrasjon.class, name = "INTEGRASJON"),
        @JsonSubTypes.Type(value = EksponertForAutorisasjon.class, name = "AUTORISASJON")
})
public interface EksponertFor {
    EksponertForType getType();
}