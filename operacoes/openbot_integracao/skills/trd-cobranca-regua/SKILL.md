---
name: trd-cobranca-regua
description: Automação determinística e execução da Régua de Cobrança B2B da TRD Consultoria de D-2 a D+15. Controla lembretes preventivos, avisos de vencimento amigáveis, aplicação de multa (2%) e juros de mora (1% a.m.), notificações extrajudiciais e bloqueio automatizado de instâncias single-tenant de clientes inadimplentes.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "Negócios / Cobrança"
---

# TRD Régua de Cobrança B2B

Esta skill padroniza os procedimentos operacionais de cobrança, cálculo de penalidades contratuais e contingenciamento de acesso para inadimplência corporativa.

## 1. Cadência Temporal da Régua (D-2 a D+15)
- **D-2 (Lembrete Preventivo):** Envio cordial de fatura por e-mail e WhatsApp com chave Pix copia-e-cola.
- **D+1 (Aviso de Vencimento):** Mensagem cordial notificando que o vencimento ocorreu ontem e solicitando o comprovante ou link atualizado.
- **D+5 (Mora & Juros):** Notificação formal com aplicação da multa contratual de 2,0% + juros de mora de 1,0% ao mês (pro-rata die).
- **D+10 (Aviso Prévio de Suspensão):** Notificação extrajudicial alertando sobre a iminência de suspensão técnica dos serviços e congelamento de SLA.
- **D+15 (Bloqueio Técnico Operacional):** Execução do bloqueio da instância single-tenant Docker do cliente e suspensão de suporte/manutenção.

## 2. Parâmetros Financeiros Pétreos
- **Multa Contratual:** 2,00% (dois por cento) sobre o valor nominal do título.
- **Juros de Mora:** 1,00% ao mês, calculados pro-rata die a partir do dia seguinte ao vencimento.
- **Canal de Envio:** WhatsApp oficial via Evolution API e e-mail corporativo institucional.
- **Desbloqueio:** Imediato após compensação bancária do valor atualizado.
