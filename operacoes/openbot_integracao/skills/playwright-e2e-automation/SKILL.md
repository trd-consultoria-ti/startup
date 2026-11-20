---
name: playwright-e2e-automation
description: Automação de testes End-to-End (E2E) com Playwright para o frontend Angular 19 PWA da TRD Consultoria. Cobre execução headless em container, testes de fluxos críticos de negócio, captura de traces e screenshots em falhas, verificação de acessibilidade e auditoria visual responsiva.
---

# Playwright E2E Automation — TRD Consultoria

## Governança de Testes End-to-End em Angular 19 PWA
O QA Gate utiliza esta skill para validar os fluxos completos do sistema antes de qualquer liberação de release.

### 1. Princípios de Confiabilidade (Anti-Flakiness)
- Localizadores semânticos baseados no usuário: preferir `getByRole`, `getByLabel`, `getByText` e `getByTestId`.
- Proibido uso de `waitForTimeout()` arbitrário. Aguardar sempre por estado de rede ou de interface: `waitForResponse()`, `toBeVisible()`.
- Resolução de layout Angular: assegurar hidratação e estabilidade de Signals antes de interações com o DOM.

### 2. Estrutura Canônica de Testes E2E
- **Fluxo de Autenticação & Sessão:** Login corporativo, renovação de token JWT e redirecionamento de rotas protegidas.
- **Fluxo de Contratos & Clientes:** Criação de novo cliente, preenchimento de formulário reativo com validações e disparo de toast de sucesso.
- **Fluxo Financeiro & Fator R:** Visualização de dashboard com gráficos SVG/Canvas e tabelas de faturamento.
- **PWA & Offline:** Validação de Service Worker, cache de assets e exibição de banner offline.

### 3. Captura de Evidências em Falha
- Em caso de falha de teste, gerar automaticamente:
  - Screenshot full-page em alta resolução (`.png`).
  - Gravação de vídeo do teste (`.webm`).
  - Playwright Trace Viewer (`trace.zip`) para depuração passo a passo de eventos de rede e console.
