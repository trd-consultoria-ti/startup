# HISTÓRICO TÉCNICO PREGRESSO DO FUNDADOR & PROJETOS CORPORATIVOS RELEVANTES (2008–2026)
### Portfólio de Engenharia Corporativa Conduzido sob Liderança Técnica de Thiago Rocha Duarte

> [!NOTE]
> **PROVENIÊNCIA E POLÍTICA DE SIGILO EMPRESARIAL:**
> A **TRD Consultoria e Serviços de TI LTDA** é uma sociedade em fase de homologação cadastral que institucionaliza e dá continuidade à prática profissional exercida por seu fundador, **Thiago Rocha Duarte**, ao longo de quase duas décadas de carreira em engenharia de software no Brasil.
> 
> Em respeito aos termos de sigilo bilateral (NDA) firmados com as empresas contratantes originais, os nomes corporativos são mantidos sob descrição setorial genérica. As métricas de volumetria, os diagnósticos de arquitetura e as soluções técnicas descritas abaixo refletem a atuação prática direta do fundador.

---

## 🏛️ PROJETO 1: Otimização de Performance e Indexação de Banco de Dados SQL de Alta Volumetria

* **Setor de Atuação:** Distribuição Atacadista, Logística e Faturamento B2B (Centro-Oeste)
* **Período:** 2018 – 2021
* **Vínculo do Fundador:** Liderança Técnica / Arquiteto de Banco de Dados
* **Stack Tecnológico:** PostgreSQL / Oracle SQL / Java Backend / Spring Data JPA
* **Volumetria Transacional:** Base superior a 1.200.000 pedidos e mais de 10 milhões de itens de estoque movimentados.

### O Desafio
A empresa registrava paradas operacionais graves em horários de pico comercial (10h às 12h e 14h às 16h). O travamento do banco de dados impedia mais de 40 representantes comerciais externos de sincronizarem pedidos através de seus coletores móveis, provocando filas na expedição e atrasos nos carregamentos de caminhões. A consultoria de infraestrutura anterior recomendava triplicar o provisionamento de servidores de nuvem, gerando custo adicional recorrente estimado em mais de R$ 6.000,00/mês.

### Diagnóstico Técnico & Resolução
1. **Auditoria de Locks e Análise de Planos de Execução:** Execução de rotinas detalhadas com `EXPLAIN ANALYZE`, identificando o disparo massivo do antipadrão *N+1 queries* em rotinas de cálculo de ICMS/ST e bloqueios de tabela (*table locks*) desnecessários em leitura.
2. **Reengenharia de Índices e Particionamento:** Criação de índices parciais compostos e particionamento de tabelas históricas de log que consumiam buffer de memória RAM indevidamente.
3. **Segregação de Carga Transacional:** Implementação de réplica de leitura (*Read Replica*) para desviar relatórios fiscais pesados da réplica primária de escrita (*Master*).

### Métricas & Resultados Aferidos
* 📉 **Redução de 98,2% no Tempo de Resposta:** Consultas críticas de faturamento que demoravam entre 14 e 18 segundos passaram a responder na faixa de **280ms a 320ms**.
* 📉 **Carga de CPU:** O servidor de banco de dados, que operava com picos constantes de **98% de CPU**, estabilizou com média de consumo em **22%**.
* 💰 **Economia Real:** Evitou o upgrade desnecessário de servidores de banco de dados, poupando mais de **R$ 70.000,00 anuais** para a operação.
* 🚀 **Continuidade:** Zero registros de travamento de faturamento nos períodos fiscais subsequentes.

---

## ⚙️ PROJETO 2: Resgate, Modernização e Sustentação de Aplicação Corporativa Legada

* **Setor de Atuação:** Cooperativa de Serviços Financeiros e Crédito
* **Período:** 2021 – 2023
* **Vínculo do Fundador:** Especialista em Engenharia de Software / Tech Lead
* **Stack Tecnológico:** Java 11 / Spring Boot / Angular / REST APIs / Docker / PostgreSQL
* **Cenário:** Sistema central de gestão operacional abandonado após rescisão contenciosa com a software house terceirizada anterior.

### O Desafio
A empresa herdou um código corporativo de 4 anos de desenvolvimento contínuo sem testes automatizados, sem documentação de arquitetura e com dependências desatualizadas com falhas de segurança conhecidas (CVEs). A diretoria temia realizar qualquer alteração nas rotinas de conciliação financeira por receio de corrupção de saldos em produção.

### Diagnóstico Técnico & Resolução
1. **Engenharia Reversa & Mapeamento de Regras:** Mapeamento integral dos fluxos de dados, serviços e endpoints através de geração automática de contratos OpenAPI (Swagger) e diagramação da arquitetura.
2. **Esteira de Blindagem Automatizada (CI/CD):** Implementação de pipeline de integração contínua (GitHub Actions) com suíte de testes de regressão automatizados para validação de regras de negócio antes de qualquer deploy em produção.
3. **Upgrade Modular e Seguro:** Atualização de dependências críticas de segurança e refatoração modular das rotinas mais instáveis, mantendo 100% de compatibilidade retroativa.

### Métricas & Resultados Aferidos
* 🛡️ **Zero Downtime em Produção:** Implementação de esteira de deploy sem interrupção de serviço (*zero downtime*).
* ⏱️ **Redução do Tempo de Resolução de Bugs (MTTR):** O tempo médio de diagnóstico e correção de chamados caiu de **48 horas para menos de 3 horas**.
* 📦 **Retomada de Novos Módulos:** O cliente pôde voltar a lançar novos recursos para seus usuários finais com total previsibilidade orçamentária e tranquilidade operacional.

---

## 🔒 PROJETO 3: Adequação Regulatória à LGPD, Blindagem de Autenticação e Gestão de Incidentes

* **Setor de Atuação:** Entidade de Classe e Registro Profissional (Brasília/DF)
* **Período:** 2023 – 2025
* **Vínculo do Fundador:** Arquiteto de Software & Consultor de Segurança
* **Stack Tecnológico:** TypeScript / Node.js / Keycloak (OAuth2 / OIDC) / PostgreSQL / Docker
* **Cenário:** Portal de associados com dados sensíveis e transações financeiras vulnerável a acessos não autorizados.

### O Desafio
Auditoria preliminar identificou fragilidades em autenticação, exposição de dados cadastrais sensíveis em endpoints públicos e ausência de trilha de auditoria para atendimento às exigências legais da Lei Geral de Proteção de Dados (Lei nº 13.709/2018).

### Diagnóstico Técnico & Resolução
1. **Autenticação Centralizada Corporativa:** Migração de autenticação legada para Keycloak com suporte a Single Sign-On (SSO), tokens JWT criptografados e autenticação em dois fatores (2FA/MFA).
2. **Isolamento de Camada e Criptografia em Repouso:** Implementação de criptografia a nível de coluna para dados pessoais sensíveis (CPF, dados bancários) e blindagem de conexões via TLS 1.3.
3. **Trilha de Auditoria Imutável:** Configuração de logs de auditoria imutáveis registrando toda e qualquer tentativa de acesso ou alteração em dados de associados.

### Métricas & Resultados Aferidos
* 🔒 **Conformidade LGPD:** 100% dos relatórios de conformidade técnica emitidos e homologados pelo comitê jurídico da entidade (Arts. 37 a 48 da LGPD).
* 🛑 **Zero Incidentes de Vazamento:** Bloqueio de mais de 12.000 requisições automatizadas maliciosas (bots) nos primeiros 30 dias de implementação do novo gateway de segurança.
* 🤝 **Satisfação dos Associados:** Aumento de 40% na utilização do portal digital pelos associados em virtude da rapidez e confiabilidade do novo login institucional.

---

### 💼 Como Estes Projetos Respaldam a Atuação da TRD:
1. **Fundamentação Histórica:** Os métodos, esteiras de testes e protocolos de SLA adotados pela TRD não são teóricos; foram lapidados ao longo de 18 anos de resolução de problemas corporativos complexos.
2. **Segurança Jurídica:** Apresentar a experiência pregressa de forma transparente e setorial demonstra maturidade e respeito estrito aos acordos de confidencialidade (NDA) anteriores.
