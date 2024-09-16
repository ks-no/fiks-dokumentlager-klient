package no.ks.fiks.dokumentlager.klient.authentication;

import no.ks.fiks.fiksporten.klient.FiksportenKlient;
import no.ks.fiks.fiksporten.klient.AccessTokenRequest;
import org.eclipse.jetty.client.Request;

import java.util.Set;
import java.util.UUID;

import static no.ks.fiks.fiksporten.klient.AccessTokenRequestKt.DEFAULT_NUMBER_OF_SECONDS_LEFT_BEFORE_EXOIRE;

public class FiksportenAuthenticationStrategy implements AuthenticationStrategy {

    private final FiksportenKlient fiksportenKlient;
    private final UUID clientId;

    public FiksportenAuthenticationStrategy(FiksportenKlient fiksportenKlient, UUID clientId) {
        this.fiksportenKlient = fiksportenKlient;
        this.clientId = clientId;
    }

    @Override
    public void setAuthenticationHeaders(Request request) {
        request.headers(headers -> headers
                .add("Authorization", "Bearer " + getAccessToken()));
    }

    private String getAccessToken() {
        return fiksportenKlient.getAccessToken(new AccessTokenRequest(clientId, Set.of("ks:fiks"), DEFAULT_NUMBER_OF_SECONDS_LEFT_BEFORE_EXOIRE));
    }
}
