# PARECER TÉCNICO DE ARQUITETURA, ENTREGA & VIABILIDADE TÉCNICA SOLO
## Avaliação de Engenharia para Expansão Nacional PME (Tech Delivery Assessment)
**TRD Consultoria e Serviços de TI LTDA — Brasília/DF**  
**Arquiteto Responsável:** Arquiteto Chefe de Entrega & Performance Técnica  
**Destinatário:** Thiago Rocha Duarte (Sócio-Fundador & Engenheiro Chefe)  
**Data:** 18 de Novembro de 2026  
**Status:** HOMOLOGADO PARA OPERAÇÃO SOLO DE ALTA PERFORMANCE  
**Versão:** `v1.0.0-PRODUCTION`

---

## 1. Sumário Executivo & Diagnóstico de Viabilidade

O Plano de Expansão Nacional para Pequenas e Médias Empresas (`PLANO_EXPANSAO_NACIONAL_PME.md`), integrado à Ficha Técnica do MVP Starter R$ 9.800 (`ANEXO_I_FICHA_TECNICA_STARTER_9800.md`), ao Contrato com Staging TRD Gate (`CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md`) e ao Relatório de Auditoria Crítica (`AUDITORIA_CETICA_PLANO_EXPANSAO_NACIONAL_PME.md`), foi submetido a rigoroso escrutínio de engenharia de software, dimensionamento de esforço e arquitetura de entrega.

### Diagnóstico Central:
A execução técnica de projetos Starter (R$ 9.800) em regime de **Solo Founder** no prazo de **15 a 20 dias úteis** é **100% FACTÍVEL, ECONOMICAMENTE VIÁVEL E SUSTENTÁVEL**, desde que ancorada em dois pilares invioláveis:
1. **A Máquina 80/20 (TRD Core Boilerplate):** Zero codificação de infraestrutura básica ou componentes genéricos do zero. O esforço do engenheiro concentra-se estritamente nos 20% de regras de negócio customizadas do cliente.
2. **Governança de Custódia & Escopo (Staging TRD Gate + SPOC):** Nenhuma linha de código transferida antes da liquidação financeira integral e nenhum atendimento a operadores descentralizados.

Abaixo, detalha-se o parecer técnico minucioso, cronograma dia a dia, arquitetura de componentes, protocolo de nuvem R$ 0 e runbook de onboarding.

---

## 2. Viabilidade do Prazo de 15 a 20 Dias Úteis (Solo Founder)

### 2.1. Análise da Complexidade Ciclotímica das 4 Telas do Starter (R$ 9.800)

| Tela | Componentes & Regras | Complexidade Bruta (Zero Boilerplate) | Complexidade Líquida (Com TRD Core Boilerplate) | Esforço Técnico Estimado |
| :--- | :--- | :---: | :---: | :---: |
| **Tela 1: Auth & RBAC** | Login JWT, recuperação por e-mail, RBAC (Admin/Operador), listagem de usuários (máx 15). | Média (18h) | **Baixa (2h)** — 100% herdado do boilerplate; personaliza-se apenas a identidade visual. | 2 horas |
| **Tela 2: Clientes** | CRUD completo, busca com debounce, paginação de 20/pág, validação de CPF/CNPJ, soft-delete. | Média (14h) | **Baixa (4h)** — Utiliza o componente genérico `<trd-data-table>` e formulário reativo padrão. | 4 horas |
| **Tela 3: Pedidos / OS + PDF** | Itens dinâmicos, cálculo reativo de totais, máquina de estados (Pendente ➔ Em Execução ➔ Concluído ➔ Cancelado), geração de PDF de espelho para impressão/WhatsApp. | Alta (26h) | **Média (12h)** — Máquina de estados configurada via enum JPA; engine de PDF Thymeleaf/OpenHTMLtoPDF pré-formatada. | 12 horas |
| **Tela 4: Dashboard & Financeiro** | Agregação SQL de contas a receber por status (A Vencer, Recebido, Atrasado), total do mês, quitação manual, exportação CSV. | Média (16h) | **Baixa-Média (6h)** — View SQL / Native Query indexada no Postgres; exportador CSV genérico do boilerplate. | 6 horas |
| **Subtotal de Codificação:** | **—** | **74 horas** | **24 horas de desenvolvimento líquido** | **24 horas** |

### 2.2. O Modelo Matemático da Capacidade Solo (Thiago Duarte)
Considerando a dedicação diária de **4 horas técnicas focadas em desenvolvimento** (Deep Work matutino, reservando a tarde para reuniões comerciais, alinhamentos e suporte SLA de contratos ativos):
* **20 dias úteis × 4h/dia = 80 horas técnicas disponíveis por ciclo mensal.**
* O desenvolvimento líquido do Starter consome **24 a 28 horas técnicas**.
* O tempo restante (**~52 horas**) é alocado para: modelagem de banco, testes de carga, provisionamento de infraestrutura, reuniões assíncronas com o SPOC, homologação e folga operacional contra imprevistos.
* **Conclusão:** O prazo de 15 a 20 dias úteis possui uma folga de segurança superior a **60%**, tornando o risco de atraso por complexidade técnica estatisticamente desprezível.

### 2.3. Cronograma Executivo Dia a Dia (20 Dias Úteis)

```mermaid
gantt
    title Cronograma de Entrega Starter MVP (20 Dias Úteis)
    dateFormat  D
    axisFormat Dia %d
    section Fase 1: Arquitetura & Setup
    Fork Boilerplate & Modelagem SQL     :active, d1, 1d
    Entities, DTOs & Services JPA        :d2, 2d
    REST Endpoints & Testes Unitários    :d4, 1d
    Engine PDF & Queries Financeiras     :d5, 1d
    section Fase 2: Construção Frontend
    Tela 1 (Auth) & Tela 2 (Clientes)    :d6, 2d
    Tela 3 (Pedidos/OS & Itens Dinâmicos):d8, 2d
    Tela 3 (PDF Blob) & Tela 4 (Dashboard):d10, 2d
    section Fase 3: Staging & Homologação
    Deploy Staging TRD & Seed de Dados   :d12, 1d
    Auditoria de Performance & CORS/Sec  :d13, 1d
    Vídeo Demo SPOC (Loom 5 min)         :d14, 1d
    Janela de Homologação SPOC (5 dias)  :d15, 5d
    section Fase 4: Go-Live & Handover
    Termo Gov.br & Liquidação 50% Finais :d19, 1d
    Virada Produção + Loom 15 min Handover:d20, 1d
```

#### Detalhamento Operacional Dia a Dia:

* **Dia 01 — Kickoff & Fundação Relacional:** Criação do repositório privado via fork do `trd-core-boilerplate`. Execução do Flyway Migration `V1__init_schema.sql` (criação das tabelas `tb_users`, `tb_roles`, `tb_clientes`, `tb_pedidos`, `tb_pedido_itens` e `tb_financeiro_lancamentos`).
* **Dia 02 — Backend Domain & Repositories:** Mapeamento das entidades Jakarta Persistence (JPA), criação dos Spring Data Repositories com queries derivadas e projeções DTO imutáveis (`Java Records`).
* **Dia 03 — Business Logic & REST Services:** Implementação dos Services `@Transactional` com validações via Bean Validation (`@NotNull`, `@CPF`, `@CNPJ`). Configuração do ciclo de vida dos pedidos com travas de integridade.
* **Dia 04 — Motor de PDF & Agregação SQL:** Integração do template Thymeleaf de espelho de OS com OpenHTMLtoPDF. Implementação de Native Query indexada para consolidação do dashboard financeiro com execução sub-15ms.
* **Dia 05 — Setup do Frontend Angular 18+:** Clone do shell do cliente, importação dos tokens visuais da empresa (cores primárias e logotipo), geração de interfaces TypeScript espelho dos DTOs e testes do Interceptor JWT.
* **Dia 06 — Telas 1 & 2 (Auth & Clientes):** Validação da tela de login e ajuste do CRUD de clientes com busca reativa e máscara de formatação para telefone e documentos.
* **Dia 07 — Tela 2 (Refinamento):** Testes de paginação assíncrona, filtros por status e validação de duplicidade de CNPJ/CPF com feedback visual acessível.
* **Dia 08 — Tela 3 (Pedidos/OS - Formulário Dinâmico):** Construção da interface com Angular Signals para adição/remoção reativa de itens, atualização instantânea de valor total e seleção de condições de pagamento.
* **Dia 09 — Tela 3 (Pedidos/OS - Gestão de Estados):** Listagem de pedidos, pills coloridos de status (Pendente / Em Execução / Concluído / Cancelado) e acionamento do endpoint de geração de PDF com abertura imediata em aba isolada ou download direto.
* **Dia 10 — Tela 4 (Dashboard Financeiro do Dono):** Montagem dos 3 KPI Cards (A Vencer, Recebido, Atrasado), filtros de competência (Mês Atual / Mês Anterior) e rotina de exportação instantânea para CSV formatado em padrão brasileiro (UTF-8 com BOM).
* **Dia 11 — Pipeline de CI/CD & Deploy no Staging TRD:** Disparo do workflow do GitHub Actions para compilação estática do Angular e empacotamento do container backend na nuvem de staging da TRD (`homologacao-[cliente].trdconsultoria.com.br`).
* **Dia 12 — Carga de Dados Simulação (Seed Data):** Injeção de massa de dados realista no banco de testes (20 clientes fictícios, 40 pedidos distribuídos em vários status) para validar comportamento visual e responsividade móvel.
* **Dia 13 — Stress-Test & Hardening de Segurança:** Auditoria de headers HTTP (HSTS, Content-Security-Policy), validação de CORS estrito, testes de injeção de payload malicioso e aferição de latência dos endpoints (meta: < 150ms).
* **Dia 14 — Liberação do Staging & Vídeo Demo Preliminar (5 min):** Gravação de demonstração guiada pelo fundador explicando os fluxos implementados e envio formal do acesso de homologação ao SPOC com início do protocolo de aceite.
* **Dia 15 a 17 — Janela de Homologação Assistida:** O SPOC executa testes práticos no sistema diretamente pelo navegador. Thiago mantém vigilância passiva e atende chamados exclusivamente por texto e prints.
* **Dia 18 — Ajustes Finais de Escopo & Conclusão de Homologação:** Correção de eventuais apontamentos funcionais dentro da Ficha Técnica. SPOC emite o aceite formal.
* **Dia 19 — O Gate Financeiro & Jurídico:** Envio do *Termo de Homologação, Aceite e Entrega Definitiva* para assinatura eletrônica via Gov.br. Liquidação bancária dos 50% finais (compensação de Pix/Boleto ou confirmação de captura do cartão).
* **Dia 20 — Go-Live Automatizado & Entrega do Vídeo-Tutorial de 15 min:** Execução do pipeline de deploy para a infraestrutura do cliente, apontamento de DNS corporativo, emissão da NFS-e final e envio do link do Loom definitivo de onboarding.

---

## 3. Arquitetura do TRD Core Boilerplate (A Máquina 80/20)

Para blindar o modelo operacional contra dívida técnica e retrabalho, o **TRD Core Boilerplate** foi arquitetado como uma plataforma modular pré-testada com cobertura de código > 85% nos componentes comuns.

```mermaid
flowchart TB
    subgraph Frontend ["Frontend: Angular 18+ Standalone"]
        UI_Shell["Shell Responsivo\n(Sidebar + Header + Breadcrumb)"]
        UI_Signals["State Management\n(Angular Signals + Computed)"]
        UI_Tables["<trd-data-table>\n(Paginação, Busca, Sort, Debounce)"]
        UI_Forms["Formulários Reativos\n(Máscaras BRL, Validação CPF/CNPJ)"]
        UI_PDF["PDF Viewer & Blob Streamer"]
    end

    subgraph Backend ["Backend: Spring Boot 3.3 (Java 21 LTS)"]
        SEC["Spring Security 6\n(Stateless JWT + RBAC)"]
        AUDIT["Auditoria Automática\n(CreatedDate, LastModified, User)"]
        EX_HANDLER["Global Exception Handler\n(RFC 7807 Problem Details)"]
        PDF_SVC["PdfReportService\n(OpenHTMLtoPDF + Thymeleaf)"]
        CSV_SVC["CsvExportUtility\n(Stream UTF-8 BOM)"]
        ACTUATOR["Spring Actuator\n(Health & Metrics para Uptime Kuma)"]
    end

    subgraph Data ["Banco de Dados: PostgreSQL 16"]
        FLYWAY["Flyway Migrations Engine\n(V1__base, V2__tenant_rules)"]
        INDEXES["B-Tree Indexes Otimizados\n(Buscas textuais & Status)"]
        AUDIT_LOG["Tabela de Auditoria Histórica"]
    end

    Frontend <==>|"REST JSON over TLS 1.3"| Backend
    Backend <==>|"HikariCP Connection Pool"| Data
```

### 3.1. Stack Tecnológica Recomendada: Justificativa de Engenharia de Missão Crítica

* **Backend: Spring Boot 3.3 / Java 21 (LTS) Enterprise:**
  * *Por que não Node.js/NestJS?* Embora Node.js seja popular, o ecossistema Spring Boot 3 com Java 21 oferece robustez transacional incomparável (`@Transactional` com isolamento ACID real), segurança corporativa madura (Spring Security 6 nativo), compilação com verificação estrita de tipos via Java Records e gerenciamento de concorrência com Virtual Threads (Project Loom), garantindo baixíssimo consumo de recursos sem o risco de crash por exceções não tratadas comuns no event-loop de Node.
  * O ecossistema Java 21 LTS confere autoridade instantânea ao cliente PME, que reconhece o padrão tecnológico de bancos, Serasa e governo.
* **Frontend: Angular 18+ (Standalone Components, Signals & Zero CDN):**
  * *Por que Angular e não React?* O Angular moderno fornece uma arquitetura "batteries-included" com roteamento, injeção de dependência, cliente HTTP com interceptors e formulários reativos nativos padronizados. Elimina o caos de dependências externas dispersas do ecossistema React.
  * O uso de **Signals** garante performance reativa cirúrgica sem a sobrecarga do Zone.js legada.
  * **Zero CDN:** Fontes Inter/Geist compiladas localmente no bundle (`@font-face` WOFF2) e ícones renderizados como SVGs vetoriais inline eliminam qualquer vulnerabilidade por queda de CDNs públicas ou bloqueios de rede local do cliente.
* **Banco de Dados: PostgreSQL 16 Dedicado:**
  * O padrão de ouro do mercado corporativo. Suporte nativo a tipos JSONB para flexibilidade de configurações, queries com índices B-Tree compostos e total compatibilidade com extensões de auditoria.

### 3.2. Catálogo de Componentes Pré-Construídos no Repositório Base

1. **Segurança & Sessão:**
   * Filtro `JwtAuthenticationFilter` com validação de assinatura criptográfica HMAC-SHA256.
   * Mecanismo de renovação automática de sessão via Refresh Token armazenado em Cookie HTTP-Only seguro.
   * Anotações de controle de acesso `@PreAuthorize("hasRole('ADMIN')")` e `@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")`.
2. **Camada de Apresentação (Angular Component Library TRD):**
   * `<trd-data-table>`: Componente genérico que recebe um Signal de dados e renderiza colunas, paginação no servidor, ordenação e barra de busca com debounce configurável de 300ms.
   * `<trd-kpi-card>`: Card responsivo de métricas com formatação de moeda brasileira (`Intl.NumberFormat('pt-BR')`), ícone contextual e indicador de tendência.
   * `<trd-status-badge>`: Badge visual estilizado para estados operacionais com cores semânticas WCAG AAA (Pendente: Âmbar; Concluído: Esmeralda; Cancelado: Carmim).
3. **Motor de Geração de Documentos (PDF Engine):**
   * Classe `PdfReportService` pré-integrada com motor OpenHTMLtoPDF. Renderiza layouts HTML5 responsivos com CSS Print em documentos PDF vetoriais em menos de 80ms, contendo logotipo do cliente, tabela zebrada de itens, totais discriminados e código hash de validação.
4. **Resiliência & Monitoramento:**
   * Configuração de endpoints de auditoria `/actuator/health` expondo status da conexão JDBC e uso de memória da JVM, compatível com monitoramento Uptime Kuma.

---

## 4. Protocolo de Deploy & Ambiente Staging (Staging TRD Gate)

A custódia técnica exigida pelo auditor cético é a salvaguarda operacional da TRD contra calotes remotos e disputas financeiras. Abaixo é detalhada sua implementação técnica e o modelo financeiro de R$ 0 de passivo de nuvem.

### 4.1. Arquitetura de Isolamento do Staging TRD

```
Ambiente de Desenvolvimento Local (Thiago)
                 │
                 ▼ (git push origin staging)
GitHub Actions Workflow (CI/CD TRD)
                 │
                 ├────────────────────────────────────────────────┐
                 ▼                                                ▼
  [Backend Deploy: Railway / Render]              [Frontend Deploy: Cloudflare Pages]
  Container JVM: prj-cliente-staging.up.railway.app  Bundle Estático: homologacao-[cliente].trdconsultoria.com.br
                 │                                                │
                 └───────────────────────┬────────────────────────┘
                                         ▼
            Acesso Restrito ao SPOC via Credenciais de Teste
             (Banco Isolado em Cluster Seguro sob Gestão da TRD)
```

* **Domínio de Homologação:** Subdomínio padronizado `homologacao-[cliente].trdconsultoria.com.br` com certificado SSL Wildcard corporativo da TRD via Cloudflare.
* **Isolamento de Dados:** Cada projeto Starter em homologação possui seu próprio banco PostgreSQL isolado ou schema restrito, impedindo contaminação cruzada.
* **Posse de Código:** O repositório Git permanece estritamente privado sob a organização `github.com/trdconsultoria`. Nenhum commit, binário ou acesso SSH/FTP é concedido ao cliente nessa fase.

### 4.2. O Modelo Nuvem R$ 0 (Passivo Financeiro Zero para a TRD)

Para assegurar que a TRD jamais arque com custos recorrentes de hospedagem dos clientes:

```mermaid
sequenceDiagram
    autonumber
    actor SPOC as Gestor do Cliente (SPOC)
    participant TRD as Thiago Duarte (TRD)
    participant Cloud as Provedor Nuvem (Railway / Supabase)

    Note over SPOC,TRD: Etapa D-0: Onboarding & Setup Financeiro de Nuvem
    TRD->>SPOC: Envia Guia de Criação de Conta na Nuvem (Railway / Supabase)
    SPOC->>Cloud: Cria conta oficial com CNPJ e cadastra Cartão Corporativo do Cliente
    SPOC->>Cloud: Convida Thiago (contato@trdconsultoria.com.br) como Membro Técnico / Dev
    Note over Cloud: Faturamento mensal (US$ 5 a US$ 10) cobrado 100% no cartão da Contratante
    
    Note over TRD,Cloud: Durante o Projeto: TRD desenvolve e testa no Staging TRD
    
    Note over SPOC,TRD: Etapa D-20: Go-Live & Virada Definitiva
    TRD->>SPOC: Apresenta Homologação Concluída
    SPOC->>TRD: Assina Termo Gov.br + Liquida Parcela Final de 50%
    TRD->>Cloud: Executa Script de Deploy direto na conta provisionada do cliente
    TRD->>SPOC: Aponta domínio oficial (app.cliente.com.br) + Entrega Código Git
```

1. **Protocolo no Onboarding (Dia 01):** O SPOC recebe o manual executivo de 2 páginas da TRD para criação da conta no provedor de nuvem selecionado (ex: Railway, Supabase ou DigitalOcean). O cliente insere seu próprio cartão corporativo.
2. **Isolamento de Titularidade:** A TRD atua estritamente com privilégios de `Deployer / Developer`. A fatura mensal de consumo de infraestrutura (estimada entre US$ 5,00 e US$ 15,00/mês para porte Starter) é debitada automaticamente no cartão da PME.
3. **Inadimplência de Infraestrutura:** Caso a empresa cliente cancele seu cartão ou não pague a nuvem, a suspensão do serviço é executada pelo próprio provedor de nuvem (Railway/Supabase), sem que haja qualquer cobrança judicial ou responsabilidade civil contra a TRD Consultoria.

### 4.3. Pipeline de Virada para Produção (The Production Switch)

O script automatizado de release só é disparado após a validação criptográfica de duas condições prévias:
* **Gatilho Jurídico:** Termo de Homologação e Entrega Definitiva assinado com carimbo ICP-Brasil via Gov.br arquivado no cofre da TRD.
* **Gatilho Financeiro:** Baixa efetiva no extrato bancário PJ (Pix ou compensação do boleto Asaas) ou autorização irrevogável da operadora de cartão de crédito.

**Execução Técnica do Go-Live (15 minutos):**
1. O workflow `deploy-production.yml` é executado informando as variáveis de ambiente da nuvem do cliente.
2. O Flyway executa as migrations estruturais no banco PostgreSQL definitivo do cliente.
3. O build compilado do frontend é publicado no Cloudflare Pages / Vercel corporativo do cliente.
4. É realizada a checagem dos registros CNAME e emissão de SSL do domínio corporativo (`app.empresa.com.br`).
5. Thiago realiza a transferência formal de posse do repositório Git (ou entrega do `.bundle` autenticado), arquiva o snapshot no cofre frio e encerra a fase de obra.

---

## 5. Treinamento Assíncrono & Onboarding (Loom 15 Minutos)

Para exterminar a dependência operacional, impedir que o fundador vire helpdesk N1 e garantir a autonomia da PME, a entrega oficial do sistema é acompanhada de um vídeo instrucional com estrutura pedagógica rígida de 15 minutos:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│               ROTEIRO ESTRUTURADO DO VÍDEO DE ONBOARDING TRD (15 MINUTOS)               │
├─────────────┬───────────────────────────┬──────────────────────────────────────────────┤
│ Minutagem   │ Módulo em Tela            │ Conteúdo & Instrução Operacional             │
├─────────────┼───────────────────────────┼──────────────────────────────────────────────┤
│ 00:00-01:30 │ Bloco 1: Acesso & Perfis   │ Como fazer login seguro; recuperação de      │
│             │                           │ senha via e-mail; diferença prática entre    │
│             │                           │ perfil Administrador e Operador.             │
├─────────────┼───────────────────────────┼──────────────────────────────────────────────┤
│ 01:30-04:30 │ Bloco 2: Gestão Clientes   │ Cadastro completo de clientes (CPF/CNPJ);    │
│             │                           │ preenchimento de endereço e WhatsApp;        │
│             │                           │ uso da busca instantânea e paginação.        │
├─────────────┼───────────────────────────┼──────────────────────────────────────────────┤
│ 04:30-09:30 │ Bloco 3: Pedidos, OS      │ Criação de pedidos com itens dinâmicos;      │
│             │ & Emissão de PDF          │ cálculo automático de valores; fluxo dos     │
│             │                           │ status (Pendente -> Concluído); geração e    │
│             │                           │ impressão do Espelho em PDF para cliente.    │
├─────────────┼───────────────────────────┼──────────────────────────────────────────────┤
│ 09:30-12:30 │ Bloco 4: Dashboard Dono   │ Leitura dos cards financeiros; conferência   │
│             │ & Exportação CSV          │ de contas a receber; quitação de recebíveis  │
│             │                           │ e exportação da planilha para o Excel/CSV.   │
├─────────────┼───────────────────────────┼──────────────────────────────────────────────┤
│ 12:30-15:00 │ Bloco 5: Governança, SLA  │ Regras da Garantia de 30 dias (somente bugs  │
│             │ & Próximos Passos         │ de código); canal exclusivo via SPOC com     │
│             │                           │ texto e prints; apresentação da Fase 2 no    │
│             │                           │ Plano Continuidade TRD (R$ 1.800/mês).       │
└─────────────┴───────────────────────────┴──────────────────────────────────────────────┘
```

### Impacto Operacional do Vídeo:
* **Autonomia da Equipe do Cliente:** Qualquer novo funcionário admitido pela PME assiste ao vídeo gravado por Thiago, eliminando horas de reuniões de treinamento síncronas.
* **Barreira contra Falsos Chamados:** Se o operador acionar o gestor alegando dúvida de preenchimento, o SPOC responde enviando o minuto exato do vídeo institucional.

---

## 6. Veredito Técnico & Recomendações de Engenharia

### 6.1. Nota Técnica da Arquitetura & Esteira de Entrega
# **Nota Técnica de Delivery: 9.9 / 10**
*(O modelo atinge maturidade industrial de engenharia de software: reduz drasticamente o esforço braçal através do TRD Core Boilerplate, elimina o passivo de infraestrutura com nuvem no cartão do cliente e aniquila o risco financeiro e de suporte com o Staging TRD Gate e a barreira do SPOC).*

---

### 6.2. As 5 Recomendações Mandatórias de Engenharia para Thiago Duarte

1. **Inviolabilidade do Flyway Migrations:**  
   É expressamente proibido realizar qualquer alteração de estrutura no PostgreSQL via ferramentas gráficas (DBeaver, pgAdmin) em produção. Toda e qualquer alteração de schema deve ser formalizada em script de migration versionado (`V1__...sql`, `V2__...sql`), garantindo repetibilidade de 100% entre ambiente local, staging e produção.
2. **Contrato de API Tipado (OpenAPI / Swagger):**  
   Manter a documentação OpenAPI gerada automaticamente pelo Springdoc no backend. Isso permite a validação instantânea dos DTOs no frontend e serve como memorial descritivo vivo do sistema perante o cliente.
3. **Observabilidade Passiva com Uptime Kuma:**  
   Todo projeto em produção deve ter o endpoint `/actuator/health` cadastrado no Uptime Kuma da TRD, configurado com alertas em tempo real no Telegram/WhatsApp de Thiago. Se o banco do cliente sofrer degradação, a TRD é informada antes do próprio cliente.
4. **Política de Backups Automatizados & Retenção:**  
   Configurar no provedor de nuvem (Railway/Supabase) backups diários automatizados com retenção de 7 dias e habilitar script de dump semanal (`pg_dump`) para um bucket seguro S3 ou Cloudflare R2 sob custódia do cliente.
5. **Hard Limit Operacional de 2 Obras Simultâneas:**  
   Em nenhuma hipótese iniciar o desenvolvimento de um 3º projeto antes da homologação final do 1º. Clientes excedentes devem ser alocados em **Lista de Espera Oficial com início programado**, preservando a qualidade de missão crítica e a integridade física do engenheiro.

---

**Conclusão do Parecer:**  
O Plano de Expansão Nacional PME da TRD Consultoria de TI está tecnicamente homologado, operacionalmente blindado e apto para execução imediata em escala nacional.

**Arquiteto Chefe de Entrega & Performance Técnica**  
*TRD Consultoria e Serviços de TI LTDA*  
Brasília - DF, 18 de Novembro de 2026.
