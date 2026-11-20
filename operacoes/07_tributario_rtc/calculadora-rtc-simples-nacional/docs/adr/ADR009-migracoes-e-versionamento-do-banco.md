# ADR009 - Migrações e Versionamento do Banco de Dados

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

O banco SQLite de referência é gerado por migrações Flyway, separadas da aplicação. A aplicação
abre o banco somente em leitura (ver ADR001), por isso não executa migrações.

## Decisão

- O Flyway é executado por meio do `flyway-maven-plugin` declarado no [pom.xml](../../pom.xml), com o goal `flyway:migrate`.
- Os scripts ficam no repositório público `calculadora-rtc-regime-regular`, no diretório `flyway/`: `flyway/flyway-pro.conf` (configuração) e `flyway/sql/` (scripts).
- Em `flyway/sql/criacao/` ficam o baseline `B0001` e os callbacks `beforeMigrate.sql` e `afterMigrate.sql`. Em `flyway/sql/manutencao/` ficam os scripts versionados.
- A versão pública gera o banco `calculadora-pro.db` em `./calculadora/db/` com `mvn flyway:migrate -Dflyway.configFiles=./flyway/flyway-pro.conf`, conforme o workflow de publicação do repositório público.
- O banco gerado é usado pelo perfil `offline` (ver ADR001).

## Consequências

Positivas:
- A aplicação não tem permissão de escrita nem lógica de migração
- Geração do banco é reprodutível por comando único

Negativas:
- Os scripts Flyway não estão neste repositório: a geração do banco depende do repositório público `calculadora-rtc-regime-regular` (ver Decisão)
- O fluxo de geração do banco depende de execução manual do comando Maven antes da primeira subida no perfil `offline`

## Notas

- Introdução do `flyway-maven-plugin`: 24/07/2026.
- Verificação: o diretório `flyway/` existe no branch `main` do repositório público `calculadora-rtc-regime-regular`, com `flyway-pro.conf`, `sql/criacao/` e `sql/manutencao/`.
