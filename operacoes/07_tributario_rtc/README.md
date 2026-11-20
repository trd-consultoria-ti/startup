# Operações 07 — Módulos Tributários da Reforma do Consumo (RTC / Receita Federal)

> **TRD Consultoria e Serviços de TI LTDA**  
> **CNPJ:** 43.065.207/0001-57  
> **Governança:** FinOps, Fator R, Planejamento Tributário & Compliance Fiscal  
> **Base Legal:** Emenda Constitucional nº 132/2023 · Lei Complementar nº 214/2025 (RTC - IBS / CBS)

---

## 0. Modelo de Oportunidade Comercial & Oferta aos Clientes

> 💼 **Oportunidade de Mercado & Playbook de Negócios:**  
> A adequação à Reforma Tributária do Consumo (EC 132/2023 · LC 214/2025) representa um dos maiores vetores de geração de receita da TRD Consultoria. Estruturamos uma oferta comercial completa e playbook de implementação corporativa para empresas que necessitam adequar seus sistemas de faturamento e ERPs legados aos novos tributos (IBS / CBS / Split Payment) utilizando diretamente os motores oficiais da Receita Federal.
> 
> 📄 **Consulte o documento executivo da oferta:**  
> 👉 [**MODELO_OPORTUNIDADE_SERVICO_RTC_CLIENTES.md**](MODELO_OPORTUNIDADE_SERVICO_RTC_CLIENTES.md) — Diagnóstico Fiscal (R$ 4,8k a R$ 6,8k), Implementação Turnkey Starter RTC (R$ 9,8k a R$ 18,5k) e Sustentação Regulatória com SLA (R$ 2,8k a R$ 5,5k/mês).

---

## 1. Visão Geral e Alinhamento Estratégico

Este diretório abriga os motores oficiais de cálculo e geração documental disponibilizados pela **Secretaria Especial da Receita Federal do Brasil (RFB)** para a **Reforma Tributária do Consumo (RTC)**.

Para a **TRD Consultoria**, este arsenal de engenharia tributária possui utilidade estratégica direta em três pilares do negócio:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        MOTORES TRIBUTÁRIOS RTC — RECEITA FEDERAL                       │
├────────────────────────────┬────────────────────────────┬──────────────────────────────┤
│ 1. Simples Nacional        │ 2. Regime Regular          │ 3. Gerador DANFSe Nacional   │
│ (CBS / IBS / LC 214/2025)  │ (Lucro Presumido e Real)   │ (Go / PDF a partir do XML)   │
├────────────────────────────┼────────────────────────────┼──────────────────────────────┤
│ • Blindagem do Fator R     │ • Simulação de impacto     │ • Emissão fiduciária de      │
│ • Folha >= 28% (Anexo III) │   tributário para clientes │   notas de serviço da TRD    │
│ • Cálculo exato de CBS/IBS │   corporativos B2B da TRD  │ • Geração de DANFSe PDF em   │
│   na transição 2026-2033   │ • Compliance de tomadores  │   milissegundos com Go puro  │
└────────────────────────────┴────────────────────────────┴──────────────────────────────┘
```

---

## 2. Catálogo dos Repositórios Oficiais Integrados

### A. [`calculadora-rtc-simples-nacional/`](./calculadora-rtc-simples-nacional/)
- **Descrição Oficial:** API REST oficial da Receita Federal para cálculo da CBS (Contribuição sobre Bens e Serviços) e do IBS (Imposto sobre Bens e Serviços) nas operações de empresas enquadradas no Simples Nacional (LC 214/2025).
- **Stack Técnica:** Java 21 / Spring Boot 3 / Maven / Swagger OpenAPI (`http://localhost:8082/api/swagger-ui.html`).
- **Endpoints Chave:**
  * `POST /api/calculadora/observabilidade/simples-nacional`: Cálculo granular com erros por item.
  * `GET /api/calculadora/simples-nacional/atividades`: Consulta de atividades por Anexo e vigência.
- **Aplicação na TRD:** Validação em tempo real do Simples Nacional, monitoramento de segregação de receitas e suporte algorítmico à skill `trd-fator-r-finops`.

---

### B. [`calculadora-rtc-regime-regular/`](./calculadora-rtc-regime-regular/)
- **Descrição Oficial:** API REST oficial da Receita Federal para apuração e cálculo do IBS e da CBS sob o Regime Regular (Lucro Presumido e Lucro Real).
- **Stack Técnica:** Java 21 / Spring Boot 3 / Flyway Migrations / Maven.
- **Aplicação na TRD:** Simulações e diagnósticos tributários para clientes corporativos de médio e grande porte atendidos pela TRD Consultoria, precificação de contratos de sustentação continuada e adequação de alíquotas efetivas pós-reforma.

---

### C. [`calculadora-rtc-gerador-danfse/`](./calculadora-rtc-gerador-danfse/)
- **Descrição Oficial:** Microserviço oficial em **Go** da Receita Federal para geração do **DANFSe** (Documento Auxiliar da Nota Fiscal de Serviço Eletrônica) em PDF a partir do XML da NFS-e padrão nacional.
- **Stack Técnica:** Go 1.22+ / Taskfile / Docker / HTTP Server (Porta 8080/8081).
- **Execução Rápida:**
  ```bash
  cd calculadora-rtc-gerador-danfse
  go run ./cmd/danfse
  ```
- **Aplicação na TRD:** Automatização integral da emissão e conversão de notas fiscais de serviços de TI emitidas pela TRD para seus clientes B2B, eliminando dependência de emissores manuais municipais lentos ou instáveis.

---

## 3. Matriz de Vinculação com as Skills e Operações TRD

| Repositório Integrado | Skill TRD Vinculada | Impacto no Negócio |
| :--- | :--- | :--- |
| `calculadora-rtc-simples-nacional` | `trd-fator-r-finops` | Garante apuração exata do Simples Nacional e blindagem da alíquota de 6% no Anexo III. |
| `calculadora-rtc-regime-regular` | `trd-propostas-comerciais` | Ancoragem de custos e cálculo de impostos nos contratos corporativos e propostas executivas B2B. |
| `calculadora-rtc-gerador-danfse` | `operacoes/05_administracao` | Emissão de DANFSe automatizada via pipeline CI/CD ou rotinas OpenBot de faturamento. |

---

## 4. Política de Manutenção e Atualização

1. Os fontes integrados refletem os repositórios públicos da Receita Federal sob licença oficial aberta.
2. Atualizações de versão da Receita Federal devem ser sincronizadas periodicamente nesta branch para manter as regras de validação (tabelas de alíquotas e schemas XML) permanentemente atualizadas.
