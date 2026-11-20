---
name: trd-cobranca-fator-r
description: Motor fiscal e operacional para cálculo do Fator R no Simples Nacional, retenções federais na fonte e régua de cobrança B2B da TRD Consultoria. Garante que a relação Folha/RBT12 permaneça >= 28% (alíquota 6% no Anexo III), calcula retenções de PIS/COFINS/CSLL/IRRF e automatiza a régua de cobrança com juros e suspensão de container.
---

# TRD Cobrança & Fator R — FinOps & Governança Fiscal

## 1. Monitoramento do Fator R (Simples Nacional)
A TRD Consultoria presta serviços de TI sob o CNAE 62.01-5-01 / 62.02-3-00 e é tributada pelo Simples Nacional. A alíquota efetiva depende criticamente do **Fator R**:
$$\text{Fator R} = \frac{\text{Folha de Salários com Pró-Labore dos últimos 12 meses (FS12)}}{\text{Receita Bruta Total dos últimos 12 meses (RBT12)}}$$

### Regra de Ouro Tributária:
- **Fator R >= 0,28 (28%):** Tributação pelo **Anexo III** (Alíquota inicial de **6,0%**).
- **Fator R < 0,28:** Tributação pelo **Anexo V** (Alíquota inicial de **15,5%** — aumento de 158% na carga tributária!).

### Alertas e Ações Preventivas do Agente:
- Todo dia 20 de cada mês, cruzar o faturamento projetado com o pró-labore.
- Se o Fator R estiver entre 28% e 30% (zona de alerta): emitir aviso ao Founder recomendando ajuste pontual do pró-labore do mês para blindar a alíquota em 6%.

## 2. Retenções Tributárias Federais na Fonte (B2B)
Em notas fiscais de serviço para clientes tomadores do Lucro Presumido ou Real:
- **PIS (0,65%)**, **COFINS (3,00%)**, **CSLL (1,00%)**: Retenção conjunta de **4,65%** para valores acima do teto legal (Lei 10.833/2003).
- **IRRF (1,50%)**: Serviços de tecnologia e programação.
- O agente deve conferir se o cliente tomador realizou a retenção devida e se a TRD deve descontar tais valores na emissão do boleto/Pix líquido.

## 3. Régua de Cobrança B2B Determinística
- **D-2:** Notificação preventiva cordial via WhatsApp (Evolution API) e e-mail com Pix Copia-e-Cola e PDF da nota fiscal.
- **D+1:** Lembrete de vencimento amigável.
- **D+5:** Aviso formal de mora com recálculo automático de multa moratória de 2% e juros de 1% ao mês pro-rata die.
- **D+10:** Notificação extrajudicial de sobreaviso de suspensão contratual.
- **D+15:** Comando executivo ao Ultron/Provisionamento para suspensão preventiva do container Docker single-tenant do cliente e congelamento do atendimento de chamados SLA.
