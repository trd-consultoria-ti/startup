# ADR009 - Migrações e Versionamento do Banco de Dados

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora mantém um banco de dados SQLite com dados críticos (classificações,
alíquotas, regras tributárias) que evolui com a legislação. A base precisa ser criada e
mantida de forma reproduzível em qualquer ambiente, com histórico auditável de scripts
aplicados.

A aplicação usa o SQLite em modo read-only; portanto, as migrações não podem
ocorrer no startup da aplicação via `spring.flyway` automático. São necessárias como etapa
separada de build/deploy.

Os scripts ficam em `flyway/sql/` no repositório, organizados em pastas de criação e
manutenção, com scripts de baseline, hooks before/after e scripts de manutenção versionados.

## Decisão

Usar o `flyway-maven-plugin` para versionar criação e manutenção do SQLite, executado como
etapa explícita fora do startup Spring Boot.

Os parâmetros de conexão e localização dos scripts são fornecidos pelo processo de
build/deploy conforme o ambiente. Não há migração automática na inicialização da aplicação.

Convenção de nomes dos scripts:
- Baseline/criação: `B####__descricao.sql`
- Manutenção/versionadas: `V####__descricao.sql`

## Consequências

Positivas:
- Scripts SQL versionados ficam no repositório, com histórico e checksums do Flyway
- Criação e manutenção da base seguem ordem controlada e reproduzível
- Separação clara entre manutenção da base e runtime read-only da API
- Divergências de scripts entre ambientes podem ser detectadas por validação/checksum

Negativas:
- Build/deploy precisa executar o plugin explicitamente quando houver mudança de base
- Parâmetros de conexão e localização devem ser controlados fora do `pom.xml`
- Rollback exige scripts corretivos ou restauração de backup da base
- Scripts precisam respeitar limitações de DDL do SQLite
