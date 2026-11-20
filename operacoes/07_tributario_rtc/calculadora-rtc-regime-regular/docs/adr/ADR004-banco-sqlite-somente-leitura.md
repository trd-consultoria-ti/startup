# ADR004 - Banco de Dados SQLite Somente-Leitura

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora depende de dados de referência para realizar cálculos: classificações
tributárias, alíquotas, NCM, NBS, regras de redução e tratamento tributário, informações
geográficas. Esses dados são fornecidos por fontes governamentais, alterados periodicamente
por revisão legislativa e críticos para o cálculo.

A API não precisa modificar esses dados em tempo de execução — apenas consultá-los. Manter
um banco de dados externo (PostgreSQL, MySQL) introduz dependência operacional desnecessária
para dados predominantemente imutáveis no runtime. Dados em memória (JSON, XML) exigem custo
de parsing e indexação manual menos eficiente que SQL.

## Decisão

Usar SQLite local em modo somente-leitura, configurado via URL JDBC com `mode=ro`.

O driver usado é `org.xerial:sqlite-jdbc` com dialeto Hibernate para SQLite.
O arquivo de banco de dados fica externo ao JAR, em caminho configurável por ambiente.
A manutenção da base é feita por processo separado.

## Consequências

Positivas:
- Elimina dependência de servidor de banco de dados externo para dados de referência
- `mode=ro` impede qualquer escrita acidental pela aplicação
- Consulta estruturada via SQL/JPA com índices
- Distribuição controlada: troca de dados ocorre pela substituição versionada do arquivo

Negativas:
- Atualizações exigem substituir o arquivo de base de dados usado pela aplicação
- Múltiplas instâncias precisam receber a mesma versão do arquivo
- O arquivo externo precisa estar presente no caminho esperado pelo runtime

## Notas

Referências:
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc)
- [Hibernate Community Dialects](https://github.com/hibernate/hibernate-orm/tree/main/hibernate-community-dialects)
