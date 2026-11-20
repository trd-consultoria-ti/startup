---
name: trd-evolution-api
description: Gestão da stack Docker da Evolution API para automação de WhatsApp corporativo, criação de instâncias de clientes, geração de QR Code, webhooks de mensagens e envio de notificações transacionais na TRD Consultoria. Use para tarefas de integração WhatsApp, chatbots e notificações financeiras/SLA.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "SRE / Fábrica"
---

# TRD Evolution API — Automação de WhatsApp Corporativo

Esta skill padroniza a gestão da stack Evolution API para comunicação e notificações transacionais da TRD Consultoria.

## 1. Localização e Ambiente da Stack
- **Diretório da Stack:** `trd-evolution/` (no repositório `startup` ou raiz do projeto)
- **Porta Padrão:** `8085` (API HTTP/REST)
- **Gerenciador de Frontend:** `Evolution Manager` na porta `3000`
- **Variáveis de Ambiente Chave:** `AUTHENTICATION_API_KEY`, `SERVER_URL=http://localhost:8085`

## 2. Fluxo Operacional de Instâncias
1. **Criar Instância para Cliente:**
   - Endpoint: `POST /instance/create`
   - Payload: `{"instanceName": "cliente_<id>", "token": "<hash>", "qrcode": true}`
2. **Obter QR Code para Pareamento:**
   - Endpoint: `GET /instance/connect/cliente_<id>`
   - Retorno: Base64 do QR Code para exibição no frontend ou terminal.
3. **Configurar Webhook de Mensagens Recebidas:**
   - Endpoint: `POST /webhook/set/cliente_<id>`
   - Apontar para o backend Node.js da TRD: `https://<api>/api/v1/webhooks/whatsapp`
   - Eventos habilitados: `MESSAGES_UPSERT`, `CONNECTION_UPDATE`.

## 3. Envio de Mensagens Transacionais (Notificações)
- Endpoint: `POST /message/sendText/cliente_<id>`
- Regras Pétreas de Envio:
  - Nunca enviar mensagens fora do horário comercial (08:00 às 18:00) exceto incidentes P1 de SLA.
  - Mensagens financeiras devem sempre conter o código Pix copia-e-cola e link seguro da fatura.
