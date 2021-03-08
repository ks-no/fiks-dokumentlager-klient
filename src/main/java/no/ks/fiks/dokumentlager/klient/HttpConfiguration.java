package no.ks.fiks.dokumentlager.klient;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class HttpConfiguration {
    @Builder.Default
    Duration uploadTimeout = Duration.ofHours(1);

    @Builder.Default
    Duration downloadTimeout = Duration.ofHours(1);

    @Builder.Default
    Duration idleTimeout = Duration.ofMinutes(1);
}
