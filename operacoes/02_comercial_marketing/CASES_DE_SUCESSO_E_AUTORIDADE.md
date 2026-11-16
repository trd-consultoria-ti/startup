# HISTÓRICO TÉCNICO PREGRESSO DO FUNDADOR & PROJETOS CORPORATIVOS RELEVANTES
### Portfólio de Engenharia Corporativa, Microsserviços e IA sob Liderança Técnica de Thiago Rocha Duarte

> [!NOTE]
> **PROVENIÊNCIA, MODALIDADE OPERACIONAL E SIGILO EMPRESARIAL:**
> A **TRD Consultoria** (razão social: **THIAGO ROCHA DUARTE - CONSULTORIA E SERVICOS DE TI LTDA**, CNPJ: **43.065.207/0001-57**, fundada em 10/08/2021) é a pessoa jurídica formalizada que institucionaliza e dá continuidade à prática profissional exercida por seu sócio fundador, **Thiago Rocha Duarte**.
> 
> **Compatibilidade Operacional (Regime Misto CLT e PJ):** A trajetória corporativa do fundador contempla atuações em regime de contratação direta (CLT) combinadas com a prestação de serviços de consultoria técnica especializada (PJ), com total compatibilidade de horários, entregas por marcos contratuais e estrito cumprimento dos deveres de confidencialidade.
> 
> As métricas, arquiteturas e soluções técnicas descritas abaixo refletem a atuação prática direta do fundador em grandes instituições e corporações do setor público e privado, demonstrando solidez e experiência em ambientes de missão crítica.

---

## 🏛️ PROJETO 1: Ministério da Justiça e Segurança Pública (MJSP)
### Cadastro Nacional de Pessoas Desaparecidas (CNPD) — Integrações Governamentais e Processamento Assíncrono

* **Cliente / Instituição:** Ministério da Justiça e Segurança Pública (Governo Federal)
* **Período:** Nov/2024 – Atual
* **Papel do Fundador:** Desenvolvedor Fullstack Sênior (Java / Angular)
* **Stack Tecnológico:** Java 17, Spring Boot 2.7, Angular, TypeScript, Angular Material, Hibernate Envers, Liquibase, ShedLock, Azure DevOps, Docker, SonarQube, JaCoCo, Apache POI/PDFBox.

### O Desafio
Construir e sustentar a plataforma corporativa de alcance nacional destinada ao Cadastro Nacional de Pessoas Desaparecidas, conectando bases de dados sensíveis entre órgãos de segurança pública e disponibilizando consultas para a população e autoridades policiais sob rigorosos requisitos de segurança e LGPD.

### Diagnóstico Técnico & Implementações Chave
1. **Integrações Críticas de Segurança:** Implementação de comunicação segura com os sistemas externos **SINESP** e **DENATRAN** utilizando APIs REST com autenticação OAuth2 e certificados digitais SSL.
2. **Processamento Assíncrono de Alto Desempenho:** Arquitetura orientada a processamento assíncrono com `CompletableFuture` e `ExecutorService`, otimizando operações demoradas e chamadas externas sem degradar a experiência do usuário.
3. **Rotinas Agendadas Distribuídas:** Desenvolvimento de rotinas em lote (*batch/scheduler*) coordenadas com **Spring Scheduler** e **ShedLock**, garantindo que tarefas distribuídas não colidam nem sejam executadas em duplicidade em ambientes com múltiplas réplicas/instâncias.
4. **Auditoria Histórica Rígida (LGPD):** Implementação de auditoria imutável via **Hibernate Envers**, gravando rastreabilidade completa e histórico de qualquer modificação cadastral.
5. **Painel Público Otimizado:** Interface pública em Angular com scroll infinito, paginação sob demanda e filtros avançados, além de componentes customizados e seguros para upload e validação de imagens e documentos.
6. **Governança de Banco e CI/CD:** Versionamento e migração de esquemas com **Liquibase** e esteira de CI/CD automatizada com **Azure DevOps**, **SonarQube** e relatórios de cobertura de testes com **JaCoCo**.

---

## 💳 PROJETO 2: Serasa Experian (eConsig)
### Backend de Alta Concorrência para Operações de Crédito Consignado

* **Cliente / Instituição:** Serasa Experian
* **Período:** Jan/2021 – Ago/2024 (3 anos e 8 meses)
* **Papel do Fundador:** Desenvolvedor Backend Java
* **Stack Tecnológico:** Java 17, Spring Boot, Spring Data JPA, Hibernate, APIs REST, Microsserviços, JUnit 5, Mockito, SOLID, Clean Code.

### O Desafio
Sustentar e evoluir a plataforma corporativa do **eConsig**, responsável pelo processamento, validação e roteamento de operações de crédito consignado em larga escala, onde qualquer lentidão de banco ou inconsistência transacional impacta diretamente bancos parceiros, correspondentes e milhões de tomadores de crédito.

### Diagnóstico Técnico & Implementações Chave
1. **Engenharia de Microsserviços Resiliente:** Desenvolvimento e manutenção de APIs REST e serviços de negócio em Java 17 / Spring Boot, com baixo acoplamento e estrita separação de domínios.
2. **Otimização de Persistência e Locks:** Refatoração de repositórios e queries complexas no Spring Data JPA e Hibernate, eliminando gargalos de transação e evitando deadlocks em horários de pico financeiro.
3. **Qualidade e Confiabilidade de Código:** Aplicação rigorosa de princípios SOLID e Clean Code, sustentada por testes unitários e de integração exaustivos com **JUnit 5** e **Mockito**.
4. **Integração Corporativa:** Construção de interfaces seguras e contratos de API padronizados para interoperabilidade contínua com os ecossistemas parceiros da Serasa.

---

## 🧠 PROJETO 3: LLM Router (AI Gateway Corporativo)
### Plataforma de Orquestração Inteligente de Modelos e Agentes de IA Generativa

* **Origem:** Projeto Proprietário / Arquitetura Avançada de IA da TRD
* **Período:** Maio/2025 – Jun/2026
* **Papel do Fundador:** Arquiteto de Software e Engenheiro de IA Generativa
* **Stack Tecnológico:** Java 21, Spring Boot 3, Spring AI, Spring WebFlux, Angular, Docker, Ollama, Claude API (Anthropic), Kimi API, JUnit 5, Mockito.
* **Certificações Vinculadas:** *Anthropic — Building with the Claude API* & *Anthropic — Claude Code in Action*.

### O Desafio
Desenvolver uma camada intermediária (*AI Gateway*) que permita a sistemas corporativos consumirem múltiplos modelos de linguagem (locais via Ollama ou em nuvem via Anthropic Claude) sem ficarem reféns de um único fornecedor, controlando custos, latência e segurança de dados.

### Diagnóstico Técnico & Implementações Chave
1. **Strategy Engine Dinâmica:** Módulo inteligente em Java 21 responsável por avaliar cada prompt e selecionar dinamicamente o modelo ideal (custo vs. performance) com roteamento em milissegundos.
2. **Agent Orchestrator & Pipelines:** Orquestração de agentes especializados em cadeia (*Execution Pipeline*), separando etapas de geração, validação sintática e refinamento de resposta.
3. **Compatibilidade com Padrão OpenAI:** Normalização completa de requisições e respostas seguindo o padrão da indústria, permitindo que qualquer aplicação legada use o gateway sem reescrita.
4. **Assistente Conversacional WhatsApp:** Integração ponta a ponta com mensageria instantânea para processamento automatizado de demandas de clientes através de modelos de IA.
5. **Arquitetura Limpa:** Projeto construído sob Clean Architecture com os padrões *Strategy*, *Pipeline*, *Adapter* e *Factory*.

---

## 🏛️ PROJETO 4: Comissão de Valores Mobiliários (CVM) & ALERGS
### Modernização Fullstack de Aplicações Regulatórias e Institucionais

* **Instituições:** Comissão de Valores Mobiliários (CVM) / Assembleia Legislativa do RS (ALERGS)
* **Períodos:** Maio/2023 – Jun/2024 (CVM) | Fev/2022 – Abr/2023 (ALERGS)
* **Papel do Fundador:** Desenvolvedor Fullstack Java / Angular
* **Stack Tecnológico:** Java, Spring Boot, Angular (8/12/16), TypeScript, RxJS, Angular Material, Swagger/OpenAPI, Bancos Relacionais, Git.

### O Desafio
Garantir a evolução, estabilidade e conformidade de portais e sistemas de regulação financeira e processos legislativos estaduais com código modular e documentado.

### Diagnóstico Técnico & Implementações Chave
1. **APIs REST Padronizadas:** Criação de APIs robustas com documentação viva via Swagger/OpenAPI, facilitando a auditoria e manutenção por equipes multidisciplinares.
2. **Frontend Corporativo Reutilizável:** Criação de bibliotecas de componentes no Angular com Angular Material, formulários reativos (*Reactive Forms*) e estilização desacoplada em SCSS.
3. **Resolução Ágil de Débitos Técnicos:** Correção de falhas legadas, otimização de consultas SQL e suporte N3 para sistemas de alta relevância pública.

---

### Resumo de Competências Verificáveis do Fundador

| Pilar | Tecnologias Dominadas |
|---|---|
| **Linguagens & Backend** | Java (11, 17 e 21), Spring Boot (2.7 e 3.x), Spring AI, Spring WebFlux, JPA / Hibernate, APIs REST, Microsserviços. |
| **Frontend & UI** | Angular (8, 12, 16), TypeScript, RxJS, Angular Material, Reactive Forms, SCSS, Tailwind CSS. |
| **Inteligência Artificial** | Anthropic Claude API, Claude Code, Spring AI, AI Gateway / LLM Router, Ollama, Kimi API, Roteamento de Agentes. |
| **Bancos de Dados & Governança** | PostgreSQL, Oracle SQL, Liquibase, Hibernate Envers (auditoria LGPD), ShedLock. |
| **DevOps & Qualidade** | Docker, Kubernetes, Azure DevOps, Jenkins, CI/CD, SonarQube, JaCoCo, JUnit 5, Mockito. |
