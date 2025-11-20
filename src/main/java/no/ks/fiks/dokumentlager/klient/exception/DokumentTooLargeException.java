package no.ks.fiks.dokumentlager.klient.exception;

public class DokumentTooLargeException extends RuntimeException{

    public DokumentTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DokumentTooLargeException(String message) { super(message); }

}
