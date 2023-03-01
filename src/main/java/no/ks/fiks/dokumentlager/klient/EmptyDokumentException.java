package no.ks.fiks.dokumentlager.klient;

public class EmptyDokumentException extends RuntimeException{

    public EmptyDokumentException() {
        super("Kan ikke laste opp dokument uten innhold");
    }

}
