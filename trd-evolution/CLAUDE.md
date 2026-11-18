# Evolution API

## Project Overview
Evolution API v2.3.7 — REST API for WhatsApp communication via Baileys.
Built with Node.js, Express, Prisma ORM, and TypeScript.

## Tech Stack
- **Runtime**: Node.js + TypeScript (tsx for dev, tsup for build)
- **Framework**: Express
- **ORM**: Prisma (supports PostgreSQL and MySQL)
- **WhatsApp**: Baileys (WhatsApp Web API)
- **Cache**: Redis / Local cache
- **Auth**: API key based
- **Containerization**: Docker + docker-compose

## Project Structure
```
src/
  main.ts              — Bootstrap & Express server
  config/              — Environment, logger, event config
  api/
    controllers/       — REST controllers
    services/          — Business logic
    repository/        — Prisma repository layer
    routes/            — Express routes
    guards/            — Auth guards
    dto/               — Data transfer objects
    integrations/      — Chatbot integrations (Anthropic, OpenAI, etc.)
    provider/          — Session providers
    abstract/          — Abstract base classes
  cache/               — Cache engine (Redis + local)
  utils/               — Utilities
  validate/            — Validation schemas
  exceptions/          — Custom exceptions
prisma/
  postgresql-schema.prisma
  mysql-schema.prisma
  migrations/
```

## Development Commands
- `npm run dev:server` — Start dev server with hot reload
- `npm run build` — Type-check + bundle with tsup
- `npm run start:prod` — Start production server
- `npm run lint` — ESLint fix
- `npm run db:generate` — Generate Prisma client
- `npm run db:migrate:dev` — Run Prisma migrations (dev)
- `npm run db:deploy` — Deploy Prisma migrations

## Code Conventions
- TypeScript with path aliases (`@api/`, `@config/`, `@utils/`, etc.)
- CommonJS module system
- ESLint + Prettier enforced
- Husky pre-commit hooks (commitlint + lint)
- Prisma schema selected via `DATABASE_PROVIDER` env var

## Agent Skills Available
490+ skills installed in `.claude/skills/` and `.agents/skills/`.

### Key Skills for This Project
- **code-review** — Review code changes
- **tdd** / **test-driven-development** — Test-driven development workflow
- **deep-research** — Research before implementing
- **investigate** — Investigate bugs and issues
- **systematic-debugging** — Structured debugging
- **claude-api** — Claude/Anthropic SDK reference (for chatbot integrations)
- **design-review** — UI/UX review
- **code-simplifier** — Simplify code
- **verification-before-completion** — Verify before declaring done
- **security-guidance** — Security best practices (plugin)
- **context7** — Context lookup (plugin)
- **eli5** — Explain in simple terms
- **i-have-adhd** — ADHD-friendly output formatting

## Environment
See `.env.example` for required environment variables.
Key configs: `DATABASE_PROVIDER`, `REDIS_ENABLED`, `AUTHENTICATION_API_KEY`, etc.
