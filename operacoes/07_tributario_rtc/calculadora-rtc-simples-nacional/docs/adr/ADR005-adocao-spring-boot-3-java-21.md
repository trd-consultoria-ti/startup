# ADR005 - Adoção de Spring Boot 3 e Java 21

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

O projeto é um serviço HTTP em Java com Spring Boot. Este ADR trata da execução com o perfil `offline`.

## Decisão

- Parent `spring-boot-starter-parent` na versão `3.5.16` ([pom.xml](../../pom.xml)).
- Propriedade `java.version` igual a `21` no `pom.xml`.
- Threads virtuais habilitadas com `spring.threads.virtual.enabled: true` ([application-offline.yml](../../src/main/resources/application-offline.yml)).

## Consequências

Positivas:
- Suporte a recursos do Java 21, como threads virtuais, usados no processamento paralelo (ver ADR010)
- Versões alinhadas entre build e execução

Negativas:
- Atualizações do Spring Boot ou do JDK exigem validação das decisões dependentes de versão

## Notas

- Introdução das versões no `pom.xml`: 24/07/2026.
