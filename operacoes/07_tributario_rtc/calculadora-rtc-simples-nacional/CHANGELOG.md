# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Mantenha um Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [1.0.2] - 2026-11-18

### Modificado

- `ArredondamentoUtils` passa a ser a cópia única da política numérica da Calculadora, idêntica
  byte a byte à da `api-regime-geral`. O texto da classe deixou de tratar a política como própria
  do Simples Nacional, e a classe ganhou `arredondarAliquotaPublicada` (alíquota com 4 casas) e
  `ratearValorFiscal` (parcelas que somam o total), usados pelo regime geral. Os valores
  calculados pelo Simples Nacional não mudaram.
- ADR003 reescrita: registra a conta interna exata, o arredondamento único de cada valor no
  cálculo do item e as somas sobre os valores publicados, em lugar da precisão interna de 8
  casas.

### Adicionado

- Teste `Teste_ArredondamentoUtilsCopiaUnica`: confere o SHA-256 da classe e do teste dela contra
  constantes iguais nos dois projetos. Alterar `ArredondamentoUtils` exige alterar a cópia da
  `api-regime-geral` e a constante.
- Teste `Teste_ArquiteturaArredondamento`: falha se houver `setScale(`, `RoundingMode` ou
  `MathContext` no código de produção fora de `ArredondamentoUtils`, ou `setRoundingMode` com
  modo que não seja o `MODO` da classe.

## [1.0.1] - 2026-11-18

### Segurança

- Mitigada vulnerabilidade do OpenJDK com `-XX:-UseBASE64Intrinsics`, conforme [aviso de segurança de 17/11/2026](https://openjdk.org/groups/vulnerability/advisories/2026-11-17).

## [1.0.0] - 2026-11-16

### Adicionado

- Os três endpoints de cálculo: campo `motivoEstado` (`codigo`, `descricao`) no item, presente só
  no estado `NAO_CALCULADO` — `REGIME_SOMENTE_RECEITA`, `CATIVSN_AUSENTE`, `CATIVSN_DESCONHECIDO`,
  `CATIVSN_INADMISSIVEL_NO_DOCUMENTO` e `ANEXO_SEM_ALIQUOTA_CBS` (este só no assistido). Os valores
  de `estadoItem` não mudaram.

### Modificado

- Alíquotas do PGDAS com várias linhas para o mesmo CNPJ e período (reprocessamentos, repetições
  da mesma mensagem): duas ou mais linhas não geram mais HTTP 503. A API devolve uma versão, a
  vigente: maior `DTHR_PROCESSAMENTO` (momento em que o PGDAS calculou); desempate maior `NSU`
  (ordem de chegada na RTC); depois maior `DTHR_INCLUSAO` (gravação no banco) e maior `NR_SQ`. A
  mesma linha no `GET` de alíquotas e no cálculo assistido; as demais ficam só na tabela. O
  contrato dos dois endpoints não ganhou campo.
- `dataHoraProcessamento` (`GET` de alíquotas e `resolucaoAliquotas` do assistido): passa a ser o
  instante gravado na coluna `TIMESTAMP WITH TIME ZONE`, em UTC. Antes o deslocamento da coluna
  era descartado e a hora local virava UTC: com a JVM em `America/Sao_Paulo`, a resposta saía três
  horas antes do instante real (ex.: `2026-10-07T11:20:31.47Z` em vez de `14:20:31.47Z`).
- Alíquotas do PGDAS: HTTP 503 só para falha de acesso ao banco (conexão, rede, tempo esgotado,
  recurso indisponível); outra falha da consulta passa a responder HTTP 500 (erro do sistema),
  com o detalhe só no log.
- tpDoc sem tratamento na Calculadora (VSN-015) passa a ser lido da coluna
  `TIPO_DFE.TPDF_B_POSSUI_TRATAMENTO_CALCULADORA`, vigente na data do fato gerador (hoje 77, 94,
  95 e 96, como antes); a mensagem não mudou.

- Plataforma atualizada para corrigir vulnerabilidades identificadas no Dependency-Track:
  Java 21 → **Java 25** e Spring Boot 3.5.16 → **4.1.2** (Spring Framework 7,
  Tomcat 11, Hibernate 7.4, Jackson 3, springdoc-openapi 3.2.0, JaCoCo 0.8.16). O contrato JSON
  dos endpoints foi preservado na migração da plataforma: o Jackson 3 roda com
  `spring.jackson.use-jackson2-defaults: true` e o `type` `about:blank` dos erros, que o Spring
  Framework 7 deixou de preencher, continua no corpo.
- Configuração renomeada pelo Spring Boot 4 (`application.yml`, `application-offline.yml` e
  `application-testes.yml`): opções de data do Jackson em `spring.jackson.datatype.datetime`;
  envio de traces em `management.opentelemetry.tracing.export.otlp.*`; desligamento do envio em
  `management.tracing.export.enabled`.
- Documento OpenAPI (só documentação): propriedades anuláveis passam a `type: [<tipo>, "null"]`,
  exemplos de campos de texto saem como texto e listas ganham descrição e tipo dos itens.
- Novo ADR013 para a migração de plataforma, substituindo o ADR005 sem alterar seu registro
  original de adoção de Spring Boot 3 e Java 21. ADRs de erros, documentação OpenAPI e métricas
  alinhados à migração.

### Removido

- Conjuntos chumbados de tpDoc sem tratamento: `TPDOCS_SEM_TRATAMENTO_CALCULADORA` em
  `ParametroCalculoSimplesService` e `SEM_TRATAMENTO_CALCULADORA`/`semTratamentoCalculadora()` em
  `SiglasDFeEnum` (este, inconsistente com o outro e sem uso).

## [0.0.7] - 2026-10-22

### Modificado

- Ajuste nos testes unitários para serem compatíveis com a nova versão do Banco de Dados SQLite

## [0.0.6] - 2026-10-22

### Adicionado

- Documentação exigida para publicação no Github RFB
