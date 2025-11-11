package no.ks.fiks.dokumentlager.klient;

import java.io.IOException;
import java.io.PipedInputStream;

public class DokumentlagerPipedInputStream extends PipedInputStream {

    private volatile Exception exception = null;

    @Override
    public synchronized int read() throws IOException {
        int read = super.read();
        checkException();
        return read;
    }

    @Override
    public synchronized int read(byte[] bytes, int i, int i1) throws IOException {
        int read = super.read(bytes, i, i1);
        checkException();
        return read;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int read = super.read(bytes);
        checkException();
        return read;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    private void checkException() throws IOException {
        if(exception != null) {
            if (exception instanceof RuntimeException r) throw r;
            if (exception instanceof IOException ex) throw ex;
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
