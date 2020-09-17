package no.ks.fiks.dokumentlager.klient.path;

import java.util.UUID;

public class DefaultPathHandler implements PathHandler {

    private static final String UPLOAD_BASE_PATH = "/dokumentlager/api/v1";

    @Override
    public String getUploadPath(UUID fiksOrganisasjonId, UUID kontoId) {
        return String.format("%s/%s/kontoer/%s/dokumenter/", UPLOAD_BASE_PATH, fiksOrganisasjonId, kontoId);
    }

    @Override
    public String getDeletePath(UUID fiksOrganisasjonId, UUID kontoId, UUID dokumentId) {
        return String.format("%s/%s/kontoer/%s/dokumenter/%s", UPLOAD_BASE_PATH, fiksOrganisasjonId, kontoId, dokumentId);
    }

    @Override
    public String getPublicKeyPath() {
        return String.format("%s/public-key", UPLOAD_BASE_PATH);
    }

    @Override
    public String getDownloadPath(UUID dokumentId) {
        return String.format("/dokumentlager/nedlasting/%s", dokumentId);
    }

    @Override
    public String getDownloadMetadataPath(UUID dokumentId) {
        return String.format("/dokumentlager/nedlasting/%s/metadata", dokumentId);
    }

    @Override
    public String getQueryDocumentPath(UUID fiksOrganisasjonId, UUID kontoId, String korrelasjonsid) {
        return String.format("%s/%s/kontoer/%s/dokumenter/sok/%s", UPLOAD_BASE_PATH, fiksOrganisasjonId, kontoId, korrelasjonsid);
    }

}
