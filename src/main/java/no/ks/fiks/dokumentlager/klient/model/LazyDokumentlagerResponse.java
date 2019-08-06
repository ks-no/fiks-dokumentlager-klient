package no.ks.fiks.dokumentlager.klient.model;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class LazyDokumentlagerResponse extends DokumentlagerResponse<InputStream> {

    private AtomicReference<DokumentlagerResponse<InputStream>> dokumentlagerResponse = new AtomicReference<>();
    private AtomicReference<DokumentlagerResponseRunner> dokumentlagerResponseRunner;

    public LazyDokumentlagerResponse(DokumentlagerResponseRunner dokumentlagerResponseRunner) {
        super(null, 0, null);
        this.dokumentlagerResponseRunner = new AtomicReference<>(dokumentlagerResponseRunner);
    }

    @Override
    public InputStream getResult() {
        if (dokumentlagerResponse.get() != null) {
             return dokumentlagerResponse.get().getResult();
        }
        return new LazyDokumentlagerInputStream(dokumentlagerResponseRunner, dokumentlagerResponse);
    }

    @Override
    public int getHttpStatus() {
        if (dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getHttpStatus();
    }

    @Override
    public Optional<String> getHeader(String header) {
        if (dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getHeader(header);
    }

}
