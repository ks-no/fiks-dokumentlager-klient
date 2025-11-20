package no.ks.fiks.dokumentlager.klient.exception;

public class EmptyDokumentException extends RuntimeException{

    public EmptyDokumentException() {
        super("Cannot upload document without content");
    }
}
