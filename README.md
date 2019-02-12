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
