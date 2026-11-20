---
name: trd-lgpd-dpo-toolkit
description: Ferramental técnico de compliance com a LGPD (Lei 13.709/2018) para a TRD Consultoria. Governa sanitização e anonimização de dumps PostgreSQL para ambientes de homologação/desenvolvimento, mascaramento de dados pessoais (PII) em logs de aplicação, relatórios ROPA e expurgo definitivo conforme Art. 16.
---

# TRD LGPD & DPO Toolkit — Governança e Proteção de Dados

## Diretrizes Mandatórias de Privacidade (Lei 13.709/2018)
O agente LGPD Compliance utiliza este toolkit para blindar a TRD contra vazamento de dados de clientes e assegurar total conformidade regulatória.

### 1. Higienização e Anonimização de Bancos de Dados
- **REGRA PÉTREA:** É terminantemente proibido restaurar bancos de produção em ambientes de desenvolvimento ou staging com dados pessoais reais.
- Script de anonimização obrigatório pós-restore em dev:
  - Nomes substituídos por geradores sintéticos (`faker`).
  - CPFs mascarados para `000.***.***-00` ou hashes sintéticos.
  - E-mails convertidos para `dev_user_<id>@sandbox.trdti.com.br`.
  - Telefones e endereços substituídos por dados fictícios padronizados.

### 2. Mascaramento de PII em Logs de Aplicação
- Interceptors HTTP no Node.js sanitizam logs antes da gravação:
  - Campos sensíveis censurados: `password`, `token`, `credit_card`, `cpf`, `rg`, `pix_key`.
  - Mascaramento por regex de qualquer ocorrência de 11 dígitos contínuos (padrão CPF).

### 3. Atendimento aos Direitos dos Titulares (Art. 18) & Expurgo (Art. 16)
- Endpoint dedicado para confirmação de existência e extração de dados do titular em JSON portátil.
- Rotina de expurgo definitivo após rescisão contratual e decurso do prazo legal de guarda fiscal (5 anos pelo Art. 173 do CTN).
