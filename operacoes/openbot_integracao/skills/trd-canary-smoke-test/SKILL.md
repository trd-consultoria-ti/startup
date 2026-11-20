---
name: trd-canary-smoke-test
description: Bateria de testes sintéticos imediatos pós-deploy para validação de produção na TRD Consultoria. Executa verificação de status HTTP, latência de banco de dados, integridade de certificados TLS e assets do frontend Angular, acionando rollback automático instantâneo se houver quebra.
---

# TRD Canary & Post-Deploy Smoke Test — Verificação Rápida de Release

## Protocolo de Validação Pós-Deploy do Founder
O agente Monitoramento pós-deploy executa este procedimento imediatamente após o deploy em produção pelo Founder Thiago.

### 1. Bateria Sintética Pós-Deploy (Tempo Máximo: 60s)
1. **Handshake TLS & SSL:** Verificar se o certificado do domínio está ativo, válido e sem erros de cadeia.
2. **Healthcheck da API:** Disparar requisição em `/api/readyz` e validar resposta HTTP 200 com tempo de resposta < 200ms.
3. **Conexão de Banco:** Validar latência da query de checagem no PostgreSQL (< 50ms).
4. **Integridade de Assets Angular:** Baixar `main.js`, `styles.css` e `index.html` do PWA e validar código 200 sem corrupção de build ou MIME type incorreto.
5. **Autenticação Smoke:** Efetuar um login sintético com usuário de healthcheck e validar geração de token.

### 2. Regra de Rollback Automático Imediato
- Se QUALQUER um dos 5 passos acima falhar:
  - **ROLLBACK IMEDIATO:** Restaurar a imagem Docker da versão estável anterior:
    `docker tag trd-sistema-generico:previous trd-sistema-generico:latest && docker compose up -d app`
  - Emitir alerta sonoro/notificação instantânea para o Ultron e para o WhatsApp do Founder Thiago.
  - Registrar laudo de falha com log de erro para o agente Release Prepare analisar.
