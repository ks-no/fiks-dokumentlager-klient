package no.ks.fiks.dokumentlager.klient;

import no.ks.fiks.dokumentlager.klient.model.DokumentMetadataDownloadResult;
import no.ks.fiks.dokumentlager.klient.model.DokumentlagerResponse;
import no.ks.fiks.dokumentlager.klient.model.Sokeresultat;
import no.ks.fiks.dokumentlager.upload.v1.DokumentMetadataUpdate;
import no.ks.fiks.dokumentlager.upload.v1.DokumentMetadataUpdateResult;
import no.ks.fiks.dokumentlager.upload.v1.DokumentMetadataUpload;
import no.ks.fiks.dokumentlager.upload.v1.DokumentMetadataUploadResult;

import java.io.Closeable;
import java.io.InputStream;
import java.util.UUID;

public interface DokumentlagerApi extends Closeable {
    DokumentlagerResponse<DokumentMetadataUploadResult> uploadDokument(
            InputStream dokumentStream,
            DokumentMetadataUpload metadata,
            UUID fiksOrganisasjonId,
            UUID kontoId,
            boolean kryptert
    );

    DokumentlagerResponse<DokumentMetadataUpdateResult> updateDokumentMetadata(
            UUID fiksOrganisasjonId,
            UUID kontoId,
            UUID dokumentId,
            DokumentMetadataUpdate update
    );

    DokumentlagerResponse<Void> deleteDokument(
            UUID fiksOrganisasjonId,
            UUID kontoId,
            UUID dokumentId
    );

    DokumentlagerResponse<Void> deleteDokumenterByKorrelasjonsid(
            UUID fiksOrganisasjonId,
            UUID kontoId,
            UUID korrelasjonsid
    );

    DokumentlagerResponse<InputStream> downloadDokument(UUID dokumentId);

    DokumentlagerResponse<InputStream> downloadDokumentLazy(UUID dokumentId);

    DokumentlagerResponse<DokumentMetadataDownloadResult> downloadDokumentMetadata(UUID dokumentId);

    DokumentlagerResponse<Sokeresultat> sokDokumenterMedKorrelasjonsid(
            UUID fiksOrganisasjonId,
            UUID kontoId,
            UUID korrelasjonsid,
            Integer fra,
            Integer til
    );

    DokumentlagerResponse<String> getPublicKey();
}
