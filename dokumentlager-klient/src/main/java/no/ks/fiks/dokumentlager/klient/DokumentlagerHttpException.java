package no.ks.fiks.dokumentlager.klient;

public class DokumentlagerHttpException extends RuntimeException {

    private final int status;
    private final String response;

    DokumentlagerHttpException(String message, int status, String response) {
        super(message);
        this.status = status;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }
}
