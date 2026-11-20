---
name: trd-contratos-sla
description: Elaboração, revisão adversarial e auditoria jurídica de minutas contratuais B2B da TRD Consultoria. Governa cláusulas de Damage Cap (limite indenizatório estrito), matriz de SLA P1 a P4 (tempos de resposta e resolução), garantia de código, propriedade intelectual e validação de assinatura eletrônica digital via Gov.br / ZapSign.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "Comando / Jurídico"
---

# TRD Contratos Corporativos & Matriz de SLA

Esta skill estabelece os padrões contratuais indispensáveis para proteger a TRD Consultoria em fechamentos de software sob medida e sustentação continuada.

## 1. Cláusula Pétrea de Damage Cap (Limitação de Responsabilidade)
- **Teto Máximo:** A responsabilidade total e acumulada da TRD por quaisquer danos diretos, perdas ou prejuízos fica terminantemente limitada ao **valor total efetivamente pago pelo cliente nos últimos 3 (três) meses**, ou ao valor total do contrato no caso de desenvolvimento pontual.
- **Exclusão Expressa:** Ficam expressamente excluídos lucros cessantes, perdas indiretas, danos à reputação ou interrupção de negócios decorrentes de falhas em APIs terceiras ou internet.

## 2. Matriz Oficial de SLA
- **P1 (Crítico - Sistema Indisponível/Gargalo Operacional Total):**
  - Tempo de Resposta: até 1 hora.
  - Tempo de Contorno / Solução: até 4 horas.
- **P2 (Alto - Funcionalidade Crítica Degradada sem Contorno):**
  - Tempo de Resposta: até 4 horas.
  - Tempo de Contorno / Solução: até 12 horas.
- **P3 (Médio - Falha Parcial com Solução de Contorno Disponível):**
  - Tempo de Resposta: até 8 horas.
  - Tempo de Contorno / Solução: até 24 horas.
- **P4 (Baixo - Dúvidas, Ajustes Cosméticos ou Solicitações de Melhoria):**
  - Tempo de Resposta: até 24 horas.
  - Tempo de Contorno / Solução: até 72 horas úteis.

## 3. Validade Jurídica de Assinatura
- Aceite preferencial via assinatura digital qualificada **Gov.br** (Lei nº 14.063/2020) ou plataforma certificada ZapSign/ClickSign.
