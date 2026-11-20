---
name: trd-fator-r-finops
description: Cálculo, monitoramento em tempo real e auditoria preventiva do Fator R no Simples Nacional para a TRD Consultoria. Garante que a folha de pagamento com pró-labore se mantenha estritamente >= 28% do faturamento bruto dos últimos 12 meses (RBT12), blindando a alíquota em 6,0% no Anexo III e evitando a queda para 15,5% no Anexo V.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "SRE / FinOps"
---

# TRD Fator R — Blindagem Fiscal no Simples Nacional

Esta skill governa o cálculo contínuo do Fator R para manter a TRD Consultoria e Serviços de TI LTDA (CNAE de desenvolvimento de software) no **Anexo III (alíquota efetiva ~6%)**, impedindo o desenquadramento para o **Anexo V (alíquota inicial 15,5%)**.

## 1. Fórmula Matemática Mandatória
```
Fator R = (Folha de Salários com Pró-Labore dos Últimos 12 Meses) / (Receita Bruta Total dos Últimos 12 Meses - RBT12)
```

## 2. Limiares de Decisão
- **Fator R >= 0.28 (28%):** ENQUADRADO NO ANEXO III (Alíquota a partir de 6,0%). Status: **OK / Seguro**.
- **0.27 <= Fator R < 0.28:** ZONA DE ALERTA AMARELO. Requer ajuste imediato de pró-labore no mês corrente.
- **Fator R < 0.27:** ALERTA VERMELHO DE DESENQUADRAMENTO. Veto fiscal imediato. Emissão de nota fiscal adicional postergada ou aumento compensatório no pró-labore.

## 3. Diretrizes de Execução
1. Toda simulação de novos contratos recorrentes (MRR) deve projetar o impacto na RBT12.
2. Todo aumento de receita bruta exige recálculo da retirada pró-labore mínima necessária para preservar os 28%.
3. Emitir relatório mensal em formato markdown/xlsx para validação do Founder e envio à contabilidade.
