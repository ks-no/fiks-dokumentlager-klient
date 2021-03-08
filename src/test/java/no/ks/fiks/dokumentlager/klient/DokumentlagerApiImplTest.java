package no.ks.fiks.dokumentlager.klient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DokumentlagerApiImplTest {

    @Test
    @DisplayName("Test required felter på builder")
    void builderTest() {
        DokumentlagerApiImpl.DokumentlagerApiImplBuilder builder = DokumentlagerApiImpl.builder();
        NullPointerException exception1 = assertThrows(NullPointerException.class, builder::build);
        assertThat(exception1.getMessage(), containsString("uploadBaseUrl is marked non-null but is null"));

        builder.uploadBaseUrl(UUID.randomUUID().toString());
        NullPointerException exception2 = assertThrows(NullPointerException.class, builder::build);
        assertThat(exception2.getMessage(), containsString("downloadBaseUrl is marked non-null but is null"));

        builder.downloadBaseUrl(UUID.randomUUID().toString());
        NullPointerException exception3 = assertThrows(NullPointerException.class, builder::build);
        assertThat(exception3.getMessage(), containsString("authenticationStrategy is marked non-null but is null"));

        builder.authenticationStrategy(request -> { });
        builder.build();
    }

}
