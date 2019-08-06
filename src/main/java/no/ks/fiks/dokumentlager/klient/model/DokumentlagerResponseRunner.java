package no.ks.fiks.dokumentlager.klient.model;

import java.io.InputStream;

public interface DokumentlagerResponseRunner {

    DokumentlagerResponse<InputStream> run();

}
