---
name: postgres-enterprise-dba
description: Engenharia e governança de banco de dados PostgreSQL 16 nativo para a TRD Consultoria. Cobre modelagem relacional estrita, migrations node-pg-migrate, índices parciais e compostos (btree, gin, brin), análise de execução com EXPLAIN (ANALYZE, BUFFERS), connection pooling, autovacuum tuning e prevenção de table locks.
---

# PostgreSQL Enterprise DBA — TRD Consultoria

## Diretrizes Operacionais de Banco de Dados Nativo (PostgreSQL 16)
Este playbook governa as operações do DBA no PostgreSQL 16 hospedado em containers Docker da TRD, sem dependência de serviços PaaS gerenciados (como Supabase).

### 1. Modelagem & Integridade Referencial
- Chaves primárias UUIDv7 (`uuid_generate_v7()`) ou `BIGINT GENERATED ALWAYS AS IDENTITY`.
- Foreign keys sempre indexadas para prevenir table locks em `DELETE` / `UPDATE` em cascata.
- Campos de auditoria obrigatórios:
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `deleted_at TIMESTAMPTZ` (soft delete para conformidade e rastreabilidade).
- Constraints rigorosas (`CHECK`, `NOT NULL`, `UNIQUE`) definidas no banco, nunca confiadas apenas à aplicação.

### 2. Migrations Transacionais (`node-pg-migrate`)
- Toda migration deve rodar dentro de transação determinística (`BEGIN ... COMMIT`).
- Para tabelas volumosas em produção:
  - Adicionar colunas com valor padrão: no PG 16, `DEFAULT` constante não reescreve a tabela.
  - Índices em produção: `CREATE INDEX CONCURRENTLY` (fora de bloco transacional quando exigido).
  - Renomear ou remover colunas: padrão `expand-migrate-contract` em deploys separados.
- Arquivo `down` obrigatório e testado para permitir rollback imediato.

### 3. Otimização & Performance de Queries
- Nunca diagnosticar lentidão por suposição: rodar `EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS)`.
- Combate a Sequential Scans em tabelas com > 1.000 linhas.
- Índices parciais para status ativos (ex: `WHERE deleted_at IS NULL AND status = 'active'`).
- Índices GIN para colunas `JSONB` e busca textual (`tsvector`).
- Monitorar `pg_stat_statements` para identificar as queries com maior consumo de tempo e I/O.

### 4. Connection Pooling & Parâmetros de Instância
- Configuração de pool de conexões (PgBouncer ou pool interno do Node.js `pg`):
  - Limite máximo de conexões calibrado para a memória do container (evitar OOM).
  - `idleTimeoutMillis: 30000`, `connectionTimeoutMillis: 5000`.
- Parâmetros base recomendados para PG 16 em container:
  - `shared_buffers = 25% da RAM total`.
  - `work_mem = 16MB a 64MB`.
  - `maintenance_work_mem = 128MB`.
  - `wal_compression = on`.
