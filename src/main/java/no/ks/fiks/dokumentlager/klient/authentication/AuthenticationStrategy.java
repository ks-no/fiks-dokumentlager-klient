package no.ks.fiks.dokumentlager.klient.authentication;


import org.eclipse.jetty.client.Request;

public interface AuthenticationStrategy {
    void setAuthenticationHeaders(Request request);
}
