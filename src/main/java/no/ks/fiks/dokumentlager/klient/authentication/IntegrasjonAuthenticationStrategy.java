package no.ks.fiks.dokumentlager.klient.authentication;

import no.ks.fiks.maskinporten.AccessTokenRequest;
import no.ks.fiks.maskinporten.Maskinportenklient;
import org.eclipse.jetty.client.Request;

import java.util.UUID;

public class IntegrasjonAuthenticationStrategy implements AuthenticationStrategy {

    private final Maskinportenklient maskinportenklient;
    private final UUID integrasjonId;
    private final String integrasjonPassord;

    public IntegrasjonAuthenticationStrategy(Maskinportenklient maskinportenklient, UUID integrasjonId, String integrasjonPassord) {
        this.maskinportenklient = maskinportenklient;
        this.integrasjonId = integrasjonId;
        this.integrasjonPassord = integrasjonPassord;
    }

    @Override
    public void setAuthenticationHeaders(Request request) {
        request.headers(headers -> headers
                .add("Authorization", "Bearer " + getAccessToken())
                .add("IntegrasjonId", integrasjonId.toString())
                .add("IntegrasjonPassord", integrasjonPassord));
    }

    private String getAccessToken() {
        return maskinportenklient.getAccessToken(AccessTokenRequest.builder().scope("ks:fiks").build());
    }
}
