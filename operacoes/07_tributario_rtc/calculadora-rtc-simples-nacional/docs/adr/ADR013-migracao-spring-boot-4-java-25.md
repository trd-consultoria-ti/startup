# ADR013 - Migração para Spring Boot 4 e Java 25

## Data

> 17/11/2026

## Status

> Aceito

Substitui o [ADR005 - Adoção de Spring Boot 3 e Java 21](./ADR005-adocao-spring-boot-3-java-21.md).

## Contexto

O projeto é um serviço HTTP em Java com Spring Boot. A decisão original adotou Spring Boot 3 e
Java 21 para a execução com o perfil `offline`. A migração atualizou Spring Boot 3.5.16 para
4.1.2 e Java 21 para 25 para corrigir vulnerabilidades identificadas no Dependency-Track.
Este ADR substitui a decisão de plataforma do ADR005 e registra a plataforma vigente no build
e na execução, inclusive no perfil `offline`, sem alterar o registro original.

## Decisão

- Parent `spring-boot-starter-parent` na versão `4.1.2` ([pom.xml](../../pom.xml)), com Spring
  Framework 7 e Jackson 3.
- Java 25 (Temurin), com `java.version`, `maven.compiler.source` e `maven.compiler.target`
  iguais a `25` no `pom.xml`. Build e execução do pipeline usam imagens Java 25
  ([.gitlab-ci.yml](../../.gitlab-ci.yml)).
- Starters modulares `spring-boot-starter-webmvc` e `spring-boot-starter-restclient`; testes
  MVC usam `spring-boot-starter-webmvc-test`.
- Serializadores e deserializadores usam `tools.jackson`, mantendo as anotações em
  `com.fasterxml.jackson.annotation`. `spring.jackson.use-jackson2-defaults: true` preserva
  os padrões de serialização anteriores
  ([JacksonConfig](../../src/main/java/br/gov/serpro/rtc/config/JacksonConfig.java) e
  [application-offline.yml](../../src/main/resources/application-offline.yml)).
- Retentativas do download do banco e da comparação de versões usam `RetryTemplate` do
  Spring Framework 7, sem a dependência `spring-retry`, preservando tentativas e intervalos
  ([DatabaseDownloadService](../../src/main/java/br/gov/serpro/rtc/config/db/DatabaseDownloadService.java)
  e [VersaoOfflineService](../../src/main/java/br/gov/serpro/rtc/api/versao/service/VersaoOfflineService.java)).
- Lombok declarado em `annotationProcessorPaths`, pois o javac a partir do JDK 23 exige
  processadores de anotação explícitos; JaCoCo atualizado para `0.8.16`.
- Tracing usa `spring-boot-micrometer-tracing-opentelemetry` e versões gerenciadas pelo
  Spring Boot para a ponte Micrometer e o exportador OTLP. As propriedades de exportação
  ficam em `management.opentelemetry.tracing.export.otlp.*`; nos perfis `offline`, `testes`
  e `local`, `management.tracing.export.enabled: false` desliga o envio
  ([application.yml](../../src/main/resources/application.yml)).
- Threads virtuais habilitadas com `spring.threads.virtual.enabled: true` ([application-offline.yml](../../src/main/resources/application-offline.yml)).

## Consequências

Positivas:
- Preservação das threads virtuais introduzidas no Java 21, usadas no processamento paralelo (ver ADR010)
- Versões alinhadas entre build e execução
- Atualização da plataforma motivada pelos apontamentos de segurança do Dependency-Track
- Compatibilidade de serialização e retentativas mantida na migração; os ajustes de erros e
  documentação OpenAPI estão registrados nos ADR004 e ADR006

Negativas:
- Atualizações do Spring Boot ou do JDK exigem validação das decisões dependentes de versão
- Build e execução passam a exigir JDK 25; integrações precisam acompanhar as APIs modulares
  do Spring Boot 4 e os pacotes do Jackson 3
- Limites de memória de execução ajustados para `MaxMetaspaceSize=160m` e
  `CompressedClassSpaceSize=32m` ([Procfile](../../publish/Procfile))

## Notas

- Migração implementada em 13/11/2026; registrada na versão
  [1.0.0 do changelog](../../CHANGELOG.md#100---2026-11-16).
- O ADR005 permanece inalterado como registro histórico da adoção de Spring Boot 3 e Java 21.
