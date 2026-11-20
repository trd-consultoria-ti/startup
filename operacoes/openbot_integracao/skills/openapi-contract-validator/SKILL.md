---
name: openapi-contract-validator
description: Validação automatizada de conformidade de contratos de API entre a especificação Swagger/OpenAPI e a implementação real no Node.js/Express da TRD Consultoria. Previne desvios documentais (drift), valida schemas de requisição e resposta e garante contratos 100% confiáveis para o frontend Angular 19.
---

# OpenAPI Contract Validator — TRD Consultoria

## Governança de Contratos de API (Single Source of Truth)
O QA Gate e o agente Documentação utilizam esta skill para impedir que discrepâncias entre a documentação Swagger e a API real cheguem a staging ou produção.

### 1. Validação de Sintaxe e Especificação
- Validação estrutural do arquivo `openapi.yaml` ou `swagger.json` contra o padrão OpenAPI 3.0.3 / 3.1.
- Verificação de campos obrigatórios: `operationId`, `summary`, `description`, `responses`, `schemas` com tipos e exemplos.

### 2. Teste de Conformidade de Resposta
- Validação automatizada (via Schemathesis ou Prism proxy):
  - Todo endpoint retornado pelo backend Node.js deve ter seu payload JSON compatível com o schema definido na documentação.
  - Nenhum campo adicional não documentado deve ser vazado.
  - Todos os códigos de status (200, 201, 400, 401, 403, 404, 500) devem respeitar seus contratos correspondentes.

### 3. Geração de Tipos TypeScript para o Frontend
- Geração automática de interfaces para o Angular 19 a partir da spec OpenAPI:
  `npx openapi-typescript ./docs/openapi.yaml -o ../trd-front/src/app/core/models/api-contracts.ts`
- Isso garante que o frontend nunca quebre por tipagem divergente de backend.
