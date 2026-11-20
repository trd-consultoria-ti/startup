# TRD CONSULTORIA — OFERTA DE SERVIÇO E PLAYBOOK DE IMPLEMENTAÇÃO
## Adequação à Reforma Tributária do Consumo (RTC / LC 214/2025) com os Motores Oficiais da Receita Federal
**Documento Executivo de Engenharia Fiduciária, Solução Tecnológica & Proposta de Serviços B2B**  
*Autor: TRD Consultoria e Serviços de TI LTDA | CNPJ: 43.065.207/0001-57 | Brasília - DF*  
*Versão: v1.0.0-PROD | Status: Vigente & Homologado para Prospecção Comercial*  
*Base Normativa: Emenda Constitucional nº 132/2023 · Lei Complementar nº 214/2025 (IBS / CBS / Split Payment)*

---

## 1. SUMÁRIO EXECUTIVO & TESE DE VALOR

A promulgação da **Reforma Tributária do Consumo (Emenda Constitucional nº 132/2023 e Lei Complementar nº 214/2025)** impõe a mais profunda reestruturação fiscal da história corporativa brasileira. A substituição gradual de tributos legados (PIS, COFINS, ICMS, ISS) pelos novos tributos sobre o valor agregado — **IBS (Imposto sobre Bens e Serviços)** e **CBS (Contribuição sobre Bens e Serviços)** —, associada ao mecanismo bancário de **Split Payment**, obriga médias e grandes empresas a reformularem seus sistemas de faturamento até 2026.

A maioria dos ERPs legados e softwares de gestão do mercado nacional padece de **inércia de atualização**, cobrando mensalidades proibitivas e oferecendo soluções genéricas que aumentam o risco de bitributação ou autuação fiscal.

### O Diferencial Único da TRD Consultoria
Em vez de construir regras tributárias baseadas em interpretações de terceiros, a **TRD Consultoria** empacota, containeriza e integra diretamente no ambiente do cliente os **motores de cálculo e geração documental desenvolvidos e homologados pela própria Secretaria Especial da Receita Federal do Brasil (RFB)**:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                   VANTAGEM COMPETITIVA DA IMPLEMENTAÇÃO TRD                            │
├──────────────────────────┬──────────────────────────┬──────────────────────────────────┤
│ 1. Algoritmo Oficial     │ 2. Zero Vendor Lock-in   │ 3. Privacidade LGPD Offline      │
├──────────────────────────┼──────────────────────────┼──────────────────────────────────┤
│ Cálculo com as exatas    │ Containers Docker sob    │ Dados fiscais e clientes não     │
│ APIs REST da Receita     │ custódia do cliente, em  │ trafegam em nuvens de terceiros  │
│ Federal (Spring Boot 3)  │ nuvem própria ou local   │ ou plataformas SaaS fechadas     │
└──────────────────────────┴──────────────────────────┴──────────────────────────────────┘
```

---

## 2. O PROBLEMA CRÍTICO & O CUSTO DA INAÇÃO PARA O CLIENTE

A inação ou o adiamento da adequação tributária expõe a empresa contratante a quatro riscos severos de faturamento e governança:

1. **Risco de Autuação & Travamento Fiscal:** A partir de 2026, com o início das alíquotas de teste de **0,9% (CBS)** e **0,1% (IBS)**, notas fiscais emitidas sem a correta segregação de itens, regimes e bases serão sumariamente rejeitadas pelos ambientes autorizadores da SEFAZ e RFB.
2. **Incompatibilidade com o Split Payment:** O Split Payment fiduciário retém o imposto na liquidação financeira do pagamento bancário (Pix, boleto, cartão). Cálculos incorretos no XML da nota geram retenções a maior ou bloqueios de crédito para o tomador do serviço.
3. **Glosa de Créditos por Clientes Corporativos:** Empresas do Lucro Real e Presumido só poderão se creditar do IBS/CBS se a nota fiscal de seus fornecedores estiver 100% aderente ao padrão nacional. Fornecedores que não se adequarem perderão contratos corporativos.
4. **Lentidão e Custo dos Grandes Fornecedores de ERP:** Customizações em softwares tradicionais demandam filas de meses e consultorias com horas-técnicas desproporcionais, gerando dependência tecnológica desnecessária.

---

## 3. A SOLUÇÃO DA TRD: ARSENAL OFICIAL DA RECEITA FEDERAL

A TRD Consultoria implementa uma arquitetura modular desacoplada, composta por três motores da Receita Federal já integrados ao nosso repositório de engenharia (`operacoes/07_tributario_rtc/`):

### Motor 1: Calculadora RTC — Simples Nacional (RFB)
- **Tecnologia:** Java 21 / Spring Boot 3 / OpenAPI Swagger.
- **Função:** Apuração exata de IBS e CBS para optantes do Simples Nacional sob a LC 214/2025, garantindo segregação por atividade (Anexos I a V) e preservação do Fator R contábil da empresa.

### Motor 2: Calculadora RTC — Regime Regular (RFB)
- **Tecnologia:** Java 21 / Spring Boot 3 / Flyway / Arquitetura Modular.
- **Função:** Simulação e apuração de débitos e créditos de IBS/CBS para clientes enquadrados no Lucro Presumido e Lucro Real, calculando alíquotas efetivas, regimes diferenciados e transição gradual de alíquotas até 2033.

### Motor 3: Gerador de DANFSe Nacional em Milissegundos (RFB)
- **Tecnologia:** Go 1.22+ / Renderizador PDF Nativo / Taskfile / Docker.
- **Função:** Geração do Documento Auxiliar da Nota Fiscal de Serviço Eletrônica (DANFSe) em PDF de alta resolução a partir do XML padrão nacional em menos de **15 milissegundos**, eliminando dependência de emissores municipais instáveis.

---

## 4. ARQUITETURA TÉCNICA DE IMPLANTAÇÃO

A TRD implanta os motores como microsserviços autônomos na infraestrutura privada do cliente (Docker / Kubernetes / AWS / Azure / On-Premise), conectando-os ao banco de dados e ERP legado através de adaptadores leves:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        ARQUITETURA DE INTEGRAÇÃO FISCAL TRD                            │
├────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                        │
│   ┌─────────────────────┐              ┌───────────────────────────────────────────┐   │
│   │   ERP DO CLIENTE    │              │       INFRAESTRUTURA PRIVADA DO CLIENTE   │   │
│   │ (Sistemas Legados,  │              │            (CONTAINERS DOCKER TRD)        │   │
│   │  TOTVS, SAP, etc.)  │              │                                           │   │
│   └──────────┬──────────┘              │   ┌───────────────────────────────────┐   │   │
│              │                         │   │  TRD Middleware Adapter (REST)    │   │   │
│              │ [JSON / Webhook]        │   └─────────────────┬─────────────────┘   │   │
│              ▼                         │                     │                     │   │
│   ┌─────────────────────┐              │                     ▼                     │   │
│   │ Pipeline de Vendas  │─────────────▶│   ┌───────────────────────────────────┐   │   │
│   │   & Faturamento     │              │   │ Motores Oficiais Receita Federal  │   │   │
│   └─────────────────────┘              │   │ • API CBS/IBS Simples (Java 21)   │   │   │
│                                        │   │ • API CBS/IBS Regular (Java 21)   │   │   │
│                                        │   └─────────────────┬─────────────────┘   │   │
│                                        │                     │                     │   │
│                                        │                     ▼                     │   │
│                                        │   ┌───────────────────────────────────┐   │   │
│                                        │   │ Gerador DANFSe Nacional (Go)      │   │   │
│                                        │   │ Renderização PDF em < 15ms        │   │   │
│                                        │   └─────────────────┬─────────────────┘   │   │
│                                        └─────────────────────┼─────────────────────┘   │
│                                                              │                         │
│                                                              ▼                         │
│                                            ┌───────────────────────────────────┐       │
│                                            │ SEFAZ / AMBIENTE NACIONAL RFB     │       │
│                                            │ • Transmissão XML NFS-e Nacional  │       │
│                                            │ • Retenção Split Payment Bancário │       │
│                                            └───────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. PACOTES COMERCIAIS & TABELA DE INVESTIMENTO

Seguindo a política de preços transparentes e fiduciários da TRD Consultoria, estruturamos três pacotes modulares:

| Pacote Comercial | Escopo de Entrega | Prazo de Execução | Investimento Fiduciário |
| :--- | :--- | :---: | :---: |
| **Pacote 1: Diagnóstico de Impacto Fiscal & PoC** | • Mapeamento do plano de contas e catálogo de serviços<br>• Simulação de alíquotas efetivas de IBS/CBS<br>• Relatório executivo de vulnerabilidade e gap analysis<br>• Demonstração prática dos motores da RFB em sandbox | **15 dias úteis** | **R$ 4.800,00 a R$ 6.800,00**<br>*(Entrada de 50% + 50% na entrega)* |
| **Pacote 2: Implementação Turnkey Starter RTC** | • Deploy dos containers Docker dos motores da RFB<br>• Desenvolvimento dos adaptadores REST para o ERP legado<br>• Parametrização das regras tributárias do cliente<br>• Implantação do gerador DANFSe em Go<br>• Homologação em ambiente de testes da SEFAZ/RFB | **45 dias úteis** | **R$ 9.800,00 a R$ 18.500,00**<br>*(Entrada de 40% + 30% homologação + 30% Go-Live)* |
| **Pacote 3: Sustentação Regulatória & SLA Continuado** | • Suporte técnico dedicado para incidentes P1 a P4<br>• SLA de resposta de 1h (P1 crítico) a 24h (P4)<br>• Atualização contínua de alíquotas e schemas XML da RFB<br>• Banco de horas técnicas mensais incluído | **Mensal recorrente** | **R$ 2.800,00 a R$ 5.500,00/mês**<br>*(Contrato anual com aviso prévio de 30 dias)* |

> [!TIP]
> **Condição Especial de Contratação Combinada:** Clientes que contratam a implementação do **Pacote 2 (Turnkey)** recebem o valor investido no **Pacote 1 (Diagnóstico)** como crédito abatido integralmente na parcela final do projeto.

---

## 6. CRONOGRAMA EXECUTIVO DE IMPLANTAÇÃO (D-0 A D+45)

```
Semana 1-2 (D+0 a D+15)   ──  Diagnóstico, auditoria de dados e simulação em Sandbox
Semana 3-4 (D+16 a D+30)  ──  Deploy de containers Docker e desenvolvimento de conectores REST
Semana 5-6 (D+31 a D+40)  ──  Homologação integrada com ERP do cliente e testes de Split Payment
Semana 7   (D+41 a D+45)  ──  Treinamento da equipe, Go-Live assistido e emissão do Ateste Gov.br
```

---

## 7. GOVERNANÇA, GARANTIAS & SEGURANÇA JURÍDICA

1. **Garantia Técnica de 90 Dias:** Todo código de conector, middleware e adaptador desenvolvido pela TRD conta com garantia contratual incondicional de 90 dias após a homologação para correção de eventuais desvios.
2. **Propriedade Intelectual & Código-Fonte:** O cliente recebe a custódia integral dos adaptadores desenvolvidos, sem taxas ocultas de licenciamento proprietário.
3. **Formalização Jurídica Blindada:** A contratação é amparada nos instrumentos oficiais da TRD:
   - [Contrato de Desenvolvimento de Software Sob Medida](../01_juridico/CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md)
   - [Contrato de Sustentação & SLA](../01_juridico/CONTRATO_SUSTENTACAO_E_SLA.md)
   - [Acordo de Confidencialidade (NDA)](../01_juridico/ACORDO_CONFIDENCIALIDADE_NDA.md)
4. **Assinatura Digital Válida:** Formalização 100% digital com certificação ICP-Brasil / Gov.br sem custos adicionais de cartório.

---

## 8. CANAL DE ATENDIMENTO & NOVOS NEGÓCIOS

Para agendar uma sessão de Diagnóstico Preliminar de Impacto da Reforma Tributária com a equipe técnica sênior da TRD Consultoria:

* **Sócio Fundador & Responsável Técnico:** Thiago Rocha Duarte
* **E-mail Corporativo:** `contato@trdconsultoria.com.br` | `thiago.duarte@trdconsultoria.com.br`
* **WhatsApp Executivo:** +55 (61) 99952-6374
* **Website Oficial:** [https://trdconsultoria.com.br/](https://trdconsultoria.com.br/)
* **Sede:** Brasília - DF | Atendimento nacional presencial e remoto.
