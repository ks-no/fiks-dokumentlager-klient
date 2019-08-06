package no.ks.fiks.dokumentlager.klient.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class LazyDokumentlagerInputStream extends InputStream {

    private final AtomicReference<DokumentlagerResponseRunner> dokumentlagerResponseRunner;
    private final AtomicReference<DokumentlagerResponse<InputStream>> dokumentlagerResponse;

    public LazyDokumentlagerInputStream(AtomicReference<DokumentlagerResponseRunner> dokumentlagerResponseRunner, AtomicReference<DokumentlagerResponse<InputStream>> dokumentlagerResponse) {
        this.dokumentlagerResponseRunner = dokumentlagerResponseRunner;
        this.dokumentlagerResponse = dokumentlagerResponse;
    }


    @Override
    public int read() throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getResult().read();
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getResult().read(bytes, i, i1);
    }

    @Override
    public long skip(long l) throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getResult().skip(l);
    }

    @Override
    public int available() throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getResult().available();
    }

    @Override
    public void close() throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        dokumentlagerResponse.get().getResult().close();
    }

    @Override
    public synchronized void mark(int i) {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        dokumentlagerResponse.get().getResult().mark(i);
    }

    @Override
    public synchronized void reset() throws IOException {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        dokumentlagerResponse.get().getResult().reset();
    }

    @Override
    public boolean markSupported() {
        if(dokumentlagerResponse.get() == null) {
            dokumentlagerResponse.set(dokumentlagerResponseRunner.get().run());
        }
        return dokumentlagerResponse.get().getResult().markSupported();
    }
}
