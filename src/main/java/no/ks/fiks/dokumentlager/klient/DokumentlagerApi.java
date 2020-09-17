package no.ks.fiks.dokumentlager.klient;

import no.ks.fiks.dokumentlager.klient.model.*;

import java.io.Closeable;
import java.io.InputStream;
import java.util.UUID;

public interface DokumentlagerApi extends Closeable {
    DokumentlagerResponse<DokumentMetadataUploadResult> uploadDokument(InputStream dokumentStream,
                                                                       DokumentMetadataUpload metadata,
                                                                       UUID fiksOrganisasjonId,
                                                                       UUID kontoId,
                                                                       boolean kryptert);

    DokumentlagerResponse deleteDokument(UUID fiksOrganisasjonId,
                                         UUID kontoId,
                                         UUID dokumentId);

    DokumentlagerResponse<InputStream> downloadDokument(UUID dokumentId);

    DokumentlagerResponse<InputStream> downloadDokumentLazy(UUID dokumentId);

    DokumentlagerResponse<DokumentMetadataDownloadResult> downloadDokumentMetadata(UUID dokumentId);

    DokumentlagerResponse<Sokeresultat> sokDokumenterMedKorrelasjonsid(UUID fiksOrganisasjonId,
                                                UUID kontoId,
                                                String korrelasjonsid,
                                                Integer fra,
                                                Integer til);

    DokumentlagerResponse<String> getPublicKey();
}
