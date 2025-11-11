package no.ks.fiks.dokumentlager.klient.exception;

public class DokumentlagerHttpException extends RuntimeException {

    private final int status;
    private final String response;

    public DokumentlagerHttpException(String message, int status, String response) {
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
