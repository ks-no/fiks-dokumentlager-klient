package no.ks.fiks.dokumentlager.klient.path;

import java.util.UUID;

public interface PathHandler {
    String getUploadPath(UUID fiksOrganisasjonId, UUID kontoId);

    String getDeletePath(UUID fiksOrganisasjonId, UUID kontoId, UUID dokumentId);

    String getPublicKeyPath();

    String getDownloadPath(UUID dokumentId);

    String getDownloadMetadataPath(UUID dokumentId);

    String getQueryDocumentPath(UUID fiksOrganisasjonId, UUID kontoId, String korrelasjonsid);
}