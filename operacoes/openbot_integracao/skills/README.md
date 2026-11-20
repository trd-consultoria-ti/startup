# Arsenal de Skills de Elite — TRD Consultoria e Serviços de TI LTDA

Este repositório reúne o acervo oficial, cirúrgico e de alta densidade técnica de **Skills Especializadas (Agent Skills)** para o ecossistema de 16 agentes autônomos da **TRD Consultoria e Serviços de TI LTDA**.

Projetado rigorosamente sob a especificação aberta de *Agent Skills* (compatível com OpenBot, GitHub Copilot, Claude Code e Cursor), este arsenal atua sob o princípio do **Carregamento Progressivo (Progressive Disclosure)**: os agentes mantêm suas janelas de contexto enxutas, acionando o corpo dos procedimentos e scripts auxiliares exclusivamente sob demanda.

---

## 🏛️ Os 5 Pilares do Arsenal TRD

### 1. Engenharia de Software Fullstack (Fábrica TRD)
*Especialistas: Front, Back e DBA*
- `angular-developer`: Desenvolvimento Angular 19 Standalone, Signals reativos, Control Flow (`@if`, `@for`) e PWA.
- `angular-new-app`: Scaffolding ágil de novos projetos com Tailwind e PWA pré-configurados.
- `responsive-design` & `mobile-touch`: Otimização responsiva (375px a 1920px) e gestos mobile.
- `frontend-design` & `design-taste-frontend`: Interface profissional, anti-slop, limpa e executiva (padrão Linear/Stripe).
- `popular-web-designs`: Tokens e padrões visuais de 54 design systems de referência mundial.
- `verify-ui-visually`: Inspeção visual rigorosa de renderizações e capturas de tela reais.
- `api-surface-care`: Design consistente de APIs RESTful versionadas em Node.js 20+ ESM.
- `nodejs-clean-architecture`: Padrão limpo desacoplado (Controller, Service, Repository) com validação de schemas Zod em runtime.
- `postgres-enterprise-dba`: Governança de PostgreSQL 16 nativo, `EXPLAIN (ANALYZE, BUFFERS)`, índices parciais e connection pooling.
- `security-reflexes` & `security-review`: Sanitização contra XSS, CSRF, SQLi e validação rigorosa de payloads.
- `concurrency-reasoning`: Controle de concorrência, idempotência e prevenção de race conditions.
- `data-migration-safety`: Migrations seguras e reversíveis com zero downtime via `node-pg-migrate`.
- `domain-modeling`: Modelagem de entidades de domínio de negócio e regras invariantes.

### 2. DevOps, SRE & Isolamento Single-Tenant (Infraestrutura TRD)
*Especialistas: Provisionamento, Observabilidade, Backup DR e Release*
- `trd-docker-single-tenant`: Provisionamento de instâncias Docker Compose isoladas por cliente com Nginx SSL Let's Encrypt e limites de CPU/RAM.
- `trd-postgres-backup-dr`: Pipeline transacional de `pg_dump` com validação SHA-256 e teste de restore automático em container efêmero.
- `trd-sre-telemetry`: Padronização de endpoints `/healthz` e `/readyz`, logs estruturados JSON com `x-request-id` e medição de SLA.
- `trd-canary-smoke-test`: Bateria de testes sintéticos instantâneos pós-deploy do Founder com gatilho de rollback em segundos.
- `docker-management` & `containers-skill`: Ciclo de vida e orquestração de containers Docker.
- `devops-engineer`: Automação e integração contínua de ambientes.
- `incident-diagnosis` & `root-cause-debugging`: Triagem P1 a P4 de incidentes e diagnóstico de causa-raiz.
- `systematic-debugging` & `perf-sanity`: Resolução metódica de bugs e análise de performance.
- `environment-first`: Diagnóstico de ambiente antes de alterar código produtivo.
- `data-loss-guard`: Protocolo stop-and-check obrigatório para comandos destrutivos.
- `cross-platform-care`: Scripts portáveis entre Windows 11 host e containers Linux.
- `github-pr-workflow` & `git-hygiene`: Governança estrita de branches Git e Pull Requests.
- `leave-no-mess`: Limpeza mandatória de resíduos e processos após execuções.

### 3. Qualidade Adversarial & Veto de Produção (Comando TRD)
*Especialistas: Ultron, Cético e QA Gate*
- `fable-judge` & `fable-method`: Auditoria adversarial rigorosa e emissão de veto sem concessões.
- `playwright-e2e-automation`: Bateria de testes End-to-End automatizados no Angular 19 PWA com captura de traces e screenshots em falhas.
- `openapi-contract-validator`: Validação automatizada de conformidade entre Swagger/OpenAPI e a API Node.js real.
- `dogfood` & `adversarial-ux-test`: Testes exploratórios e simulação de usuário resistente à tecnologia.
- `test-driven-development` & `test-design`: Metodologia TDD e desenho de matrizes de teste E2E/unitário.
- `edge-case-sweep`: Varredura sistemática de casos limites e exceções.
- `codebase-orientation`: Reconhecimento rápido de repositórios e fluxos ponta a ponta.
- `workmanship`: Disciplina de engenharia em diagnósticos e declaração de conclusão.
- `escalate-vs-decide`: Matriz de reversibilidade para decisões autônomas vs escalonamento.
- `calibrated-recommendation`: Recomendações técnicas com calibrações de evidências.

### 4. Operações de Negócio, FinOps & Documentação (Governança TRD)
*Especialistas: FinOps IA, LGPD Compliance, Cobrança e Documentação*
- `trd-cobranca-fator-r`: Blindagem fiscal no Simples Nacional (Folha/RBT12 >= 28% no Anexo III), retenções federais e régua de cobrança B2B.
- `trd-lgpd-dpo-toolkit`: Anonimização de dumps de banco para dev, mascaramento de PII em logs e atendimento aos direitos dos titulares.
- `docx`, `pdf` & `xlsx`: Geração automatizada de laudos, propostas comerciais e relatórios financeiros executivos.
- `legal-contract-negotiation-review`: Análise adversarial de minutas e contratos corporativos.
- `untrusted-content-guard`: Blindagem contra prompt injection em entradas externas e webhooks.
- `docs-as-contract`: Sincronização estrita de documentação (Swagger, READMEs) com código em produção.
- `architecture-diagram` & `concept-diagrams`: Diagramas arquiteturais SVG/HTML profissionais em dark theme.
- `baoyu-infographic`: Infográficos de alta densidade visual (21 layouts e 21 estilos).
- `estimation-and-scoping`: Dimensionamento honesto de escopo e bloqueio de scope creep.

### 5. As 7 Skills Proprietárias de Negócio da TRD (Core Business)
*Ferramental exclusivo desenvolvido sob medida para a operação da TRD Consultoria:*
- 🟢 **`trd-evolution-api`**: Gestão da stack Docker da Evolution API para automação do WhatsApp corporativo, criação de instâncias de clientes via QR Code, webhooks e notificações transacionais.
- 🟡 **`trd-fator-r-finops`**: Monitoramento contínuo da folha de pagamento vs faturamento bruto (RBT12) para assegurar o Fator R >= 28%, blindando a tributação em 6% no Anexo III do Simples Nacional.
- 🔴 **`trd-cobranca-regua`**: Execução determinística da régua de recebíveis B2B de D-2 a D+15, com cálculo de multa (2%), juros (1% a.m.) e bloqueio automático de container single-tenant em D+15.
- 🔵 **`trd-contratos-sla`**: Minutas contratuais corporativas com cláusula pétrea de Damage Cap (limite indenizatório estrito), matriz de SLA P1 a P4 e validação de assinatura digital via Gov.br (Lei 14.063/2020) ou ZapSign.
- 🟣 **`trd-single-tenant-docker`**: Provisionamento automatizado de novas instâncias por cliente com subdomínio, certificado SSL e banco de dados PostgreSQL isolado.
- 📄 **`trd-propostas-comerciais`**: Geração de Propostas Comerciais Executivas B2B, Ficha Técnica Starter R$ 9.800 e contratos de sustentação continuada.
- 🎨 **`trd-design-system`**: Implementação das diretrizes visuais oficiais da TRD em Tailwind CSS: Obsidian Navy (`#0b0f19`), Deep Slate (`#111827`), Neural Cyan (`#06b6d4`) e Electric Cyan (`#22d3ee`).
