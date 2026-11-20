# ADR011 - Adoção de Spring Boot 4 e Java 25

## Data

> 16/11/2026

## Status

> Aceito

## Contexto

O Spring Boot e o Java da API RTC Calculadora foram atualizados para tratar vulnerabilidades
identificadas pelo Dependency Track.

O Spring Boot 4.1 suporta Java 17 até Java 26 (documentação do Spring Boot v4.1.0). O Java 25 já
é usado pelo `Dockerfile` e pelo pipeline do GitLab.

Esta decisão substitui a [ADR005](./ADR005-adocao-spring-boot-3-java-21.md).

## Decisão

Usar Spring Boot 4.1.2 e Java 25 como plataforma de execução.

- Parent do Maven em `pom.xml`: `spring-boot-starter-parent` 4.1.2
- `java.version`, `maven.compiler.release` e `maven.compiler.source`/`target` em `pom.xml`: 25
- Dependência web: `spring-boot-starter-webmvc` (substitui `spring-boot-starter-web`)
- Threads virtuais continuam habilitadas via `spring.threads.virtual.enabled` em `src/main/resources/application.yml`
- Sentry e reCAPTCHA removidos do `pom.xml`
- Tracing com `spring-boot-starter-opentelemetry`, conforme a [ADR010](./ADR010-estrategia-de-observabilidade.md)

## Consequências

Positivas:
- Plataforma com correções para as vulnerabilidades identificadas pelo Dependency Track
- Java 25 usado de forma consistente em `Dockerfile` e `.gitlab-ci.yml`
- Virtual threads continuam disponíveis; o Spring Boot recomenda Java 24 ou superior para melhor desempenho
- Dependências de tracing e de versões passam a ser gerenciadas pelo Spring Boot

Negativas:
- Requer JDK 25 no ambiente de build e execução
- O Spring Boot 4 usa Jackson 3 como padrão; os testes já usam `tools.jackson`, enquanto as anotações de `src/main` continuam em `com.fasterxml.jackson.annotation`
- Bibliotecas que dependam de APIs removidas no Spring Boot 4 precisam ser revisadas

## Notas

Referências:
- [Spring Boot v4.1.0: System Requirements](https://github.com/spring-projects/spring-boot/blob/v4.1.0/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/system-requirements.adoc)
- [Spring Boot v4.1.0: Virtual threads](https://github.com/spring-projects/spring-boot/blob/v4.1.0/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/spring-application.adoc)
- Commits da 1.7.0: `efe4c80a` (migração Spring 4.1.2), `0a87c35b` (remove Sentry), `ef672356` (remove Captcha)
