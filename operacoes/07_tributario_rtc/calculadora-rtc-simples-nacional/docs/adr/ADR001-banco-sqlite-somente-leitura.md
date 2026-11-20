# ADR001 - Banco de Dados SQLite Somente-Leitura

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

A API consulta dados de referência do Simples Nacional armazenados em um arquivo SQLite.
No perfil `offline`, o datasource é configurado em [application-offline.yml](../../src/main/resources/application-offline.yml)
com `jdbc:sqlite:file:./calculadora/db/${application.db.filename}?mode=ro&date_class=TEXT&date_string_format=yyyy-MM-dd`,
driver `org.sqlite.JDBC` e dialeto `org.hibernate.community.dialect.SQLiteDialect`.

O banco não é versionado neste repositório: ele é gerado pelos scripts Flyway do repositório público
`ReceitaFederal/calculadora-rtc-regime-regular` (ADR009) e colocado em `calculadora/db/`.

## Decisão

1. **Acesso somente-leitura.** A URL JDBC usa `mode=ro`, impedindo escrita pela aplicação.

2. **Arquivo local.** No perfil `offline`, `application.db.filename` tem o valor `calculadora-pro.db`.
   O arquivo é lido de `./calculadora/db/` e gerado pelos scripts Flyway (ADR009).

3. **Validação do nome.** [DatabaseValidator](../../src/main/java/br/gov/serpro/rtc/config/db/DatabaseValidator.java)
   verifica na inicialização que o nome não é vazio, não contém espaços e termina com `-pro.db` ou `-nonpro.db`.
   Caso contrário, a aplicação não sobe.

## Consequências

Positivas:
- A aplicação não consegue alterar os dados de referência
- Trocar a base exige substituir o arquivo em `calculadora/db/` e reiniciar a aplicação, sem recompilar

Negativas:
- A subida depende de o arquivo existir em `calculadora/db/`, o que exige executar o Flyway antes;
- Os scripts Flyway não estão neste repositório; estão no repositório público `calculadora-rtc-regime-regular` (ADR009)

## Notas

- Introdução do `mode=ro` e do datasource: 24/07/2026.
- A versão pública gera o binário SQLite `calculadora-pro.db` pelos scripts Flyway do diretório `flyway/` do repositório `calculadora-rtc-regime-regular` (ADR009).
