---
name: nodejs-clean-architecture
description: Diretrizes de arquitetura limpa, segurança e tipagem estrita para o backend Node.js 20+ ESM da TRD Consultoria. Governa validação de schemas em runtime com Zod, camadas desacopladas (Controller, Service, Repository), controle transacional de banco de dados e tratamento centralizado de erros.
---

# Node.js Clean Architecture — TRD Consultoria

## Padrões Arquiteturais de Backend (Node.js 20+ ESM & TypeScript)
O agente Back utiliza esta skill para construir APIs RESTful robustas, escaláveis e desacopladas no repositório `trd-sistema-generico`.

### 1. Separação Estrita de Camadas (SoC)
- **Controllers / Routes:** Tratam requisições HTTP, efetuam parsing de cabeçalhos e parâmetros, chamam a camada de validação e delegam aos Services. Zero regras de negócio ou queries de banco nos controllers.
- **Services (Lógica de Domínio):** Orquestram as regras de negócio, transações, integrações de e-mail/WhatsApp e cálculos de negócio (ex: juros, Fator R). Totalmente desacoplados de bibliotecas de transporte HTTP (Express `req`/`res`).
- **Repositories / Data Access:** Interagem diretamente com o PostgreSQL através do driver `pg` com queries parametrizadas. Nunca expõem abstrações de banco para o controller.

### 2. Validação Rígida em Runtime com Zod
- Todo payload de entrada (`req.body`, `req.query`, `req.params`) DEVE ser validado através de um schema Zod estrito.
- Uso mandatório de `.strict()` em schemas de mutação para rejeitar propriedades não mapeadas (*prototype pollution* / *mass assignment*).
- Tipos TypeScript inferidos diretamente dos schemas Zod: `type CreateContractInput = z.infer<typeof CreateContractSchema>;`.

### 3. Tratamento Centralizado de Exceções
- Middleware global de erros interceptando `AppError` tipados:
  - `ValidationError` (400) com lista detalhada de campos malformados.
  - `UnauthorizedError` (401) e `ForbiddenError` (403).
  - `NotFoundError` (404) e `ConflictError` (409).
  - `InternalServerError` (500) com log sanitizado (zero vazamento de stacktrace ou credenciais para o cliente).
