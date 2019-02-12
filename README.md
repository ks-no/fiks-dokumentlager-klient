# Dokumentlager Klient
Klient for å laste opp, slette og laste ned dokumenter fra Fiks Dokumentlager.

Artefakter er tilgjengelig på Maven central.

## dokumentlager-klient
```xml
<dependency>
  <groupId>no.ks.fiks</groupId>
  <artifactId>dokumentlager-klient</artifactId>
  <version>1.1.0</version>
</dependency>
```

Klienten må konfigureres med følgende:
- uploadBaseUrl - Base URL til API for opplasting, f.eks. https://api.fiks.ks.no
- downloadBaseUrl - Base URL til API for nedlasting, f.eks. https://api.fiks.ks.no
- authenticationStrategy - Implementasjon av ``AuthenticationStrategy`` som setter nødvendige headers på requests fra klienten. Default implementasjon er ``IntegrasjonAuthenticationStrategy`` som bruker Maskinporten (https://github.com/ks-no/fiks-maskinporten) til å hente access token til ``Authorization`` header, og setter ``IntegrasjonId`` og ``IntegrasjonPassord``.

### Upload
Laster opp data fra en InputStream med tilhørende metadata til en gitt konto og organisasjon. Dersom kryptert flagg settes til true, eller sikkerhetsnivå er høyere enn 3 vil klienten hente Dokumentlagerets public key og bruke denne til å kryptere før opplasting.

### Delete
Sletter dokumentet med spesifisert id fra en gitt konto og organisasjon.

### Download
Laster ned dokumentet med gitt id og returnerer en InputStream med data.

## dokumentlager-spring-boot-klient
```xml
<dependency>
  <groupId>no.ks.fiks</groupId>
  <artifactId>dokumentlager-spring-boot-klient</artifactId>
  <version>1.1.0</version>
</dependency>
```
