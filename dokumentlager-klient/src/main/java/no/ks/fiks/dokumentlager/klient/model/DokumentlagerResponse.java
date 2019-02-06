package no.ks.fiks.dokumentlager.klient.model;

import lombok.Builder;

import java.util.Map;
import java.util.Optional;

@Builder
public class DokumentlagerResponse<T> {
    private final T result;
    private final int httpStatus;
    private final Map<String, String> httpHeaders;

    public T getResult() {
        return result;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Optional<String> getHeader(String header) {
        return Optional.ofNullable(httpHeaders.get(header));
    }
}
