# Dokumentlager Klient
![GitHub License](https://img.shields.io/github/license/ks-no/fiks-dokumentlager-klient)
[![Maven Central](https://img.shields.io/maven-central/v/no.ks.fiks/dokumentlager-klient)](https://search.maven.org/artifact/no.ks.fiks/dokumentlager-klient)
![GitHub Release Date](https://img.shields.io/github/release-date/ks-no/fiks-dokumentlager-klient.svg)
![GitHub Last Commit](https://img.shields.io/github/last-commit/ks-no/fiks-dokumentlager-klient.svg)

Klient for å laste opp, slette og laste ned dokumenter fra Fiks Dokumentlager.

Artefakter er tilgjengelig på Maven central.

## Versjoner

| Versjon | Jetty versjon | Status      | 
|---------|---------------|-------------|
| 4.x     | 12.x          | Aktiv       | 
| 3.x     | 12.x          | Vedlikehold | 
| 2.x     | 11.x          | Vedlikehold |

## dokumentlager-klient
```xml
<dependency>
  <groupId>no.ks.fiks</groupId>
  <artifactId>dokumentlager-klient</artifactId>
  <version>x.x.x</version>
</dependency>
```

Klienten må konfigureres med følgende:
- uploadBaseUrl - Base URL til API for opplasting, f.eks. https://api.fiks.ks.no
- downloadBaseUrl - Base URL til API for nedlasting, f.eks. https://api.fiks.ks.no
- authenticationStrategy - Implementasjon av ``AuthenticationStrategy`` som setter nødvendige headers på requests fra klienten. Default implementasjon er ``IntegrasjonAuthenticationStrategy`` som bruker Maskinporten (https://github.com/ks-no/fiks-maskinporten) til å hente access token til ``Authorization`` header, og setter ``IntegrasjonId`` og ``IntegrasjonPassord``.

### Upload
Laster opp data fra en InputStream med tilhørende metadata til en gitt konto og organisasjon. Dersom kryptert flagg settes til true, eller sikkerhetsnivå er høyere enn 3 vil klienten hente Dokumentlagerets public key og bruke denne til å kryptere før opplasting. Dersom man legger inn maksStorrelse vil man få DokumentTooLargeException dersom dokumentet er større enn angitt størrelse i byte.

### Delete
Sletter dokumentet med spesifisert id fra en gitt konto og organisasjon.

### Download
Laster ned dokumentet med gitt id og returnerer en InputStream med data.
