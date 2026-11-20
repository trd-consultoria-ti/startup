# ADR002 - Perfil Offline e Comparador de Versões

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

A API é executada com o perfil Spring `offline`, definido em
[application-offline.yml](../../src/main/resources/application-offline.yml). Nesse cenário, a base SQLite é local
(ADR001) e a aplicação compara sua versão com a de uma instalação online, por meio do endpoint
`GET /versao/status`, disponível somente neste perfil.

A execução usa o comando `./mvnw spring-boot:run -Dspring-boot.run.profiles=offline`.

## Decisão

- O comparador é implementado por [VersaoOfflineService](../../src/main/java/br/gov/serpro/rtc/api/versao/service/VersaoOfflineService.java),
  anotado com `@Profile("offline")`. O controller correspondente, `VersaoOfflineController`, expõe `GET /versao/status`
  e também é restrito a esse perfil.
- A versão local vem de `VersaoService`, a mesma montagem do endpoint `GET /versao`, que existe em todos os perfis.
- A versão remota é consultada no endpoint `/versao` da instalação apontada por `versao.url` (variável `VERSAO_URL`).
  Timeouts: `versao.connect-timeout` 5s e `versao.read-timeout` 10s.
- Na inicialização (`ApplicationReadyEvent`), a consulta remota é repetida pelo `RetryTemplate` do Spring Framework
  (3 tentativas, intervalo de 2000 ms). Se todas falharem, o serviço registra um aviso e a aplicação continua.
- Em `GET /versao/status`, uma falha na consulta remota não gera erro: os campos remotos são devolvidos nulos.
- Valores de porta e caminho do arquivo do perfil: `server.port` 8082, `server.servlet.context-path` `/api` e
  `management.server.port` 9103. O cabeçalho do arquivo cita outras portas (ver Notas).

## Consequências

Positivas:
- Execução local com base própria; a falha da instalação online não impede a subida
- Comparação de versão disponível para quem roda localmente

Negativas:
- Sem a instalação online, a comparação de `GET /versao/status` sai sem os dados remotos

## Notas

- Introdução do `VersaoOfflineService`: 24/07/2026.
