package no.ks.fiks.dokumentlager.klient.model;

import lombok.Value;

import java.util.UUID;

@Value
public class DokumentMetadataDownloadResult {
    UUID id;
    String dokumentnavn;
    String mimeType;
    Long kryptertStorrelse;
    Long ukryptertStorrelse;
    UUID korrelasjonsid;
}
