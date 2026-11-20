# CATÁLOGO MESTRE DE ARTEFATOS & GUIA DE NAVEGAÇÃO
### TRD Consultoria e Serviços de TI LTDA
**CNPJ:** 43.065.207/0001-57 | **Sede:** Brasília/DF | **Fundador:** Thiago Rocha Duarte  
*Repositório Central de Governança, Contratos, Esteira Operacional, Motores RTC e Design System*  
*Versão: v1.0.0-PROD | Status: Oficial, Homologado e Auditado*

---

## 1. APRESENTAÇÃO INSTITUCIONAL & ESTRUTURA DO REPOSITÓRIO

O repositório [`startup`](https://github.com/trd-consultoria-ti/startup) consolida todo o patrimônio intelectual, operacional, fiduciário e de engenharia de software da **TRD Consultoria e Serviços de TI LTDA**. 

Este catálogo atua como o **Índice Mestre de Navegação**, mapeando 100% dos documentos, códigos e especificações presentes no repositório. Ele orienta diferentes perfis de decisores e operadores (Comercial, Jurídico, Operações, FinOps, Frontend e Engenharia de IA) para que encontrem imediatamente os artefatos de que necessitam.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          ARQUITETURA DE MACRO-ÁREAS TRD                                │
├──────────────────────────┬──────────────────────────┬──────────────────────────────────┤
│ 1. Governança & Lançamento│ 2. Esteira Operacional   │ 3. Tecnologia & Engenharia       │
├──────────────────────────┼──────────────────────────┼──────────────────────────────────┤
│ • Plano de Lançamento    │ • 01 Jurídico & Contratos│ • 06 Identidade Visual Spartan   │
│ • Landing Page Oficial   │ • 02 Comercial & Vendas  │ • 07 Motores Tributários RTC     │
│ • Catálogo de Artefatos  │ • 03 Social Media & B2B  │ • Squad OpenBot (17 Agentes)     │
│ • Ativos de Marca (SVG)  │ • 04 RH & Avaliação Dev  │ • trd-evolution (WhatsApp Hub)   │
│                          │ • 05 FinOps & Adm/SLA    │                                  │
└──────────────────────────┴──────────────────────────┴──────────────────────────────────┘
```

---

## 2. MAPA DE NAVEGAÇÃO POR PERSONAS

Para facilitar a leitura executiva, utilize as trilhas abaixo calibradas por objetivo de negócio:

### 💼 Trilha 1: Decisor C-Level, Vendas & Comercial (CTO, VP de TI, CEO, CFO)
*Para quem busca contratar sustentação de sistemas, desenvolver MVPs corporativos sob medida ou auditar escopos.*

1. **[Proposta Comercial Executiva](operacoes/02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.md)** ([`.docx`](operacoes/02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.docx) · [`.pdf`](operacoes/02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.pdf)): Pacotes de sustentação (Bronze, Prata, Ouro) de R$ 3,8k a R$ 8,5k/mês.
2. **[One-Pager Institucional TRD](operacoes/02_comercial_marketing/ONE_PAGER_INSTITUCIONAL_TRD.md)** ([`.docx`](operacoes/02_comercial_marketing/ONE_PAGER_INSTITUCIONAL_TRD.docx) · [`.pdf`](operacoes/02_comercial_marketing/ONE_PAGER_INSTITUCIONAL_TRD.pdf)): Visão executiva em 1 página para WhatsApp e apresentações rápidas.
3. **[Ficha Técnica Starter R$ 9.800](operacoes/02_comercial_marketing/ANEXO_I_FICHA_TECNICA_STARTER_9800.md)**: Escopo fechado de MVP corporativo em 20 dias.
4. **[Checklist de Diagnóstico de Saúde de Sistemas](operacoes/02_comercial_marketing/CHECKLIST_DIAGNOSTICO_SAUDE_SISTEMA.md)**: 15 pontos de risco para auditoria preliminar de bancos de dados e legado.
5. **[Cases de Sucesso & Autoridade](operacoes/02_comercial_marketing/CASES_DE_SUCESSO_E_AUTORIDADE.md)**: Casos fiduciários reais de otimização SQL, modernização Java e conformidade LGPD.
6. **[Questionário de Requisitos de Software](operacoes/02_comercial_marketing/QUESTIONARIO_REQUISITOS_SISTEMAS_SOB_MEDIDA.md)** ([`.pdf`](operacoes/02_comercial_marketing/QUESTIONARIO_REQUISITOS_SISTEMAS_SOB_MEDIDA.pdf)): Formulário técnico de elicitação.
7. **[Guia de Elicitação com Claude](operacoes/02_comercial_marketing/GUIA_COLABORADOR_CAPTURA_E_REQUISITOS_CLAUDE.md)** ([`.pdf`](operacoes/02_comercial_marketing/GUIA_COLABORADOR_CAPTURA_E_REQUISITOS_CLAUDE.pdf)): Condução de reuniões de discovery assistidas por IA.

---

### ⚖️ Trilha 2: Jurídico, Compliance & Governança Societária
*Para departamentos jurídicos, advogados corporativos e compliance contratual.*

1. **[Contrato de Sustentação de Sistemas & SLA](operacoes/01_juridico/CONTRATO_SUSTENTACAO_E_SLA.md)** ([`.docx`](operacoes/01_juridico/CONTRATO_SUSTENTACAO_E_SLA.docx) · [`.pdf`](operacoes/01_juridico/CONTRATO_SUSTENTACAO_E_SLA.pdf)): Minuta de suporte contínuo com *Damage Cap* e retenção de código.
2. **[Contrato de Desenvolvimento de Software Sob Medida](operacoes/01_juridico/CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md)** ([`.pdf`](operacoes/01_juridico/CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.pdf)): Contrato de criação de sistemas por marcos e proteção anti-chargeback.
3. **[Acordo de Confidencialidade (NDA)](operacoes/01_juridico/ACORDO_CONFIDENCIALIDADE_NDA.md)** ([`.docx`](operacoes/01_juridico/ACORDO_CONFIDENCIALIDADE_NDA.docx)): Proteção de propriedade industrial e dados sensíveis na fase de discovery.
4. **[Acordo de Sócios (Minuta de Vesting)](operacoes/01_juridico/ACORDO_DE_SOCIOS.md)** ([`.docx`](operacoes/01_juridico/ACORDO_DE_SOCIOS.docx)): Regramento de vesting de 4 anos e cliff de 12 meses para entrada de novos sócios.
5. **[Acordo de Parceria Comercial & SDR](operacoes/01_juridico/ACORDO_PARCERIA_COMERCIAL_E_REQUISITOS_FREELANCE.md)** ([`.pdf`](operacoes/01_juridico/ACORDO_PARCERIA_COMERCIAL_E_REQUISITOS_FREELANCE.pdf)): Comissionamento de SDRs e prospecção.
6. **[Termo de Homologação e Entrega Definitiva](operacoes/01_juridico/TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md)**: Ateste formal de aceite com renúncia a contestação retroativa.
7. **[Termo de Parceria e Comissão de Indicação](operacoes/01_juridico/TERMO_PARCERIA_E_COMISSAO_INDICACAO.md)**: Parcerias estratégicas com agências e contabilidades.
8. **[Guia de Assinatura Digital Gov.br](operacoes/01_juridico/GUIA_ASSINATURA_DIGITAL_GOVBR.md)**: Manual de assinatura eletrônica válida (Lei 14.063/20).

---

### ⚙️ Trilha 3: Operações, Suporte Crítico & FinOps
*Para gerentes de operações, líderes técnicos e responsáveis pela continuidade do negócio.*

1. **[Matriz de Severidade e SLA](operacoes/05_administracao/MATRIZ_SEVERIDADE_E_SLA.md)**: Prazos de atendimento P1 (1h), P2 (4h), P3 (8h) e P4 (24h).
2. **[Runbook de Onboarding de Novo Cliente](operacoes/05_administracao/RUNBOOK_ONBOARDING_NOVO_CLIENTE.md)**: Roteiro passo a passo para assunção de sistemas em 72h sem interrupção de SLA.
3. **[Plano de Continuidade & Backup Técnico](operacoes/05_administracao/PLANO_CONTINUIDADE_E_BACKUP_TECNICO.md)**: Mitigação de homem-chave, cofre Bitwarden e credenciamento de backup técnico.
4. **[Contrato de Sobreaviso Técnico PJ](operacoes/01_juridico/CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md)**: Minuta de suporte técnico sob demanda.
5. **[Planejamento Tributário & Financeiro](operacoes/05_administracao/PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.md)** ([`.docx`](operacoes/05_administracao/PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.docx)): Simples Nacional a 6%, blindagem do Fator R e fluxo de caixa.
6. **[Guia de Infraestrutura de E-mails Corporativos](operacoes/05_administracao/GUIA_INFRAESTRUTURA_EMAILS_CORPORATIVOS.md)**: Configuração DNS segura (MX, SPF, DKIM, DMARC).

---

### 🏛️ Trilha 4: Engenharia Tributária & Motores RTC (Receita Federal)
*Para contadores, auditores fiscais e integradores da Reforma Tributária do Consumo.*

1. **[README Oficial dos Módulos RTC](operacoes/07_tributario_rtc/README.md)**: Alinhamento estratégico da EC 132/2023 e LC 214/2025 com o negócio da TRD.
2. **[Calculadora RTC — Simples Nacional](operacoes/07_tributario_rtc/calculadora-rtc-simples-nacional/)**: API REST oficial da RFB em Java 21 / Spring Boot 3 para cálculo de CBS e IBS no Simples.
3. **[Calculadora RTC — Regime Regular](operacoes/07_tributario_rtc/calculadora-rtc-regime-regular/)**: API REST oficial da RFB em Java 21 / Spring Boot 3 para apuração de IBS/CBS no Lucro Presumido e Real.
4. **[Calculadora RTC — Gerador DANFSe Nacional](operacoes/07_tributario_rtc/calculadora-rtc-gerador-danfse/)**: Microserviço oficial em Go para geração em milissegundos do PDF do DANFSe a partir do XML da NFS-e.

---

### 🎨 Trilha 5: Tecnologia, Frontend & Design System Spartan
*Para designers de produto, desenvolvedores frontend e arquitetos de software.*

1. **[Manual de Identidade Visual TRD](operacoes/06_identidade_visual/MANUAL_DE_IDENTIDADE_VISUAL_TRD.md)**: Proporção áurea da Keystone Isométrica, paleta Pantone, Cool Rich Black e WCAG AAA.
2. **[Padrão Visual Spartan Sistemas TRD](operacoes/06_identidade_visual/PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md)**: Diretriz oficial de UI corporativa para Angular 19 / Spartan UI / Tailwind CSS.
3. **[Curadoria Frontend & Design System Cooperativo B2B](operacoes/06_identidade_visual/CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md)**: Tokens Dual-Theme (Modo Claro e Modo Escuro sem `#000000`), benchmarks *Tremor* e *Shadcn Fintech*, Signals e os 5 vetos do Cético.
4. **[tokens.json](operacoes/06_identidade_visual/tokens.json) & [tokens.css](operacoes/06_identidade_visual/tokens.css)**: Tokens W3C e variáveis CSS canônicas.
5. **[tailwind.config.js](operacoes/06_identidade_visual/tailwind.config.js)**: Configuração oficial de Tailwind pronta para uso.
6. **[LogoTRD.tsx](operacoes/06_identidade_visual/LogoTRD.tsx)**: Componente React/Next.js oficial.
7. **[Landing Page Oficial](site/README.md)** ([`site/index.html`](site/index.html)): Código-fonte da página institucional.

---

### 🤖 Trilha 6: Squad OpenBot & IA Multiagente Autônoma
*Para engenheiros de IA, desenvolvedores de automação e governança de agentes.*

1. **[README do Squad OpenBot](operacoes/openbot_integracao/README.md)**: Arquitetura de orquestração do squad de 17 agentes especialistas.
2. **[Blueprint da Máquina Empresarial TRD (5 Motores)](operacoes/openbot_integracao/BLUEPRINT_MAQUINA_EMPRESARIAL_TRD.md)**: Arquitetura dos motores de vendas, entrega, sustentação, finops e governança.
3. **[Máquina de Estados de Veto & Circuit Breaker N=2](operacoes/openbot_integracao/ARQUITETURA_MAQUINA_ESTADOS_VETO.md)**: Governança adversarial com Laudo Canônico de Veto (LCV).
4. **[Matriz Tática de Fallback em 8 Níveis](operacoes/openbot_integracao/MATRIZ_TATICA_FALLBACK_8_NIVEIS.md)** ([`.pdf`](operacoes/openbot_integracao/MATRIZ_TATICA_FALLBACK_8_NIVEIS.pdf)): Resiliência de modelos LLM a custo zero (sharding, DeepSeek, Antigravity e Ollama local em VRAM).
5. **[Matriz de Personas & Skills Especializadas (17 Agentes)](operacoes/openbot_integracao/MATRIZ_PERSONAS_SKILLS_AGENTES.md)**: Atribuições e vínculo de 72 skills homologadas.
6. **[Protocolo Canônico de Mensageria](operacoes/openbot_integracao/PROTOCOLO_CANONICO_MENSAGERIA.md)**: Comunicação assíncrona entre agentes com otimização de tokens.
7. **[Arsenal de Skills Especializadas (72 Skills)](operacoes/openbot_integracao/skills/README.md)**: Repositório com as 72 skills dos agentes.

---

## 3. INVENTÁRIO EXAUSTIVO DE DIRETÓRIOS E ARTEFATOS

```
startup/
├── CNAME                                          # Apontamento de domínio para GitHub Pages
├── LICENSE                                        # Licença de distribuição MIT
├── PLANO_DE_LANCAMENTO_EMPRESARIAL.md (.docx)     # Plano Mestre de Lançamento da TRD (D-60 a D+30)
├── README.md                                      # Apresentação institucional e porta de entrada
├── CATALOGO_ARTEFATOS.md                          # Este catálogo de artefatos e índice mestre
├── iniciar.bat                                    # Script de conveniência para inicialização
│
├── assets/                                        # Suíte oficial de marcas, avatares e vetores
│   ├── trd-lockup.png                             # Logo horizontal master em alta resolução
│   ├── trd-avatar.png                             # Avatar quadrado 512x512 para perfis
│   ├── trd-org-avatar.png                         # Avatar para organizações GitHub
│   ├── trd-favicon.png                            # Favicon de sistema
│   ├── trd-mark.png                               # Símbolo Keystone isolado
│   ├── trd-mono.png                               # Versão monocromática 1-bit
│   └── svg/                                       # Suíte vetorial SVG (10 arquivos oficiais)
│
├── site/                                          # Landing Page Oficial (trdconsultoria.com.br)
│   ├── README.md                                  # Documentação de arquitetura da landing page
│   └── index.html                                 # Página principal em Tailwind CSS e tokens TRD
│
├── trd-evolution/                                 # Hub de integração WhatsApp e mensageria
│   ├── README.md                                  # Guia de implantação do motor Evolution API
│   └── src/                                       # Código fonte TypeScript do hub
│
└── operacoes/                                     # Central de Operações da TRD Consultoria
    ├── README.md                                  # Visão consolidada das 8 frentes operacionais
    │
    ├── 01_juridico/                               # [Frente 1] Jurídico, Contratos e Governança
    │   ├── README.md                              # Guia de navegação e legislação aplicável
    │   ├── CONTRATO_SUSTENTACAO_E_SLA.md (.docx/.pdf) # Contrato de sustentação com Damage Cap
    │   ├── CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md (.pdf) # Contrato de desenvolvimento
    │   ├── ACORDO_CONFIDENCIALIDADE_NDA.md (.docx) # Acordo bilateral de confidencialidade
    │   ├── ACORDO_DE_SOCIOS.md (.docx)            # Minuta contingencial com Vesting de 4 anos
    │   ├── ACORDO_PARCERIA_COMERCIAL_E_REQUISITOS_FREELANCE.md (.pdf) # Parceria com SDRs
    │   ├── CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md # Engenheiro backup sobreaviso
    │   ├── TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md # Termo de entrega e ateste formal
    │   ├── TERMO_PARCERIA_E_COMISSAO_INDICACAO.md # Parcerias com agências e contabilidades
    │   └── GUIA_ASSINATURA_DIGITAL_GOVBR.md       # Passo a passo de assinatura digital gratuita
    │
    ├── 02_comercial_marketing/                    # [Frente 2] Comercial, Vendas e Elicitação
    │   ├── README.md                              # Guia de esteira de vendas e prospecção
    │   ├── PROPOSTA_COMERCIAL_EXECUTIVA.md (.docx/.pdf) # Proposta oficial com pacotes
    │   ├── ONE_PAGER_INSTITUCIONAL_TRD.md (.docx/.pdf) # Deck executivo de 1 página
    │   ├── ANEXO_I_FICHA_TECNICA_STARTER_9800.md  # Ficha técnica do MVP Starter em 20 dias
    │   ├── CASES_DE_SUCESSO_E_AUTORIDADE.md       # Casos reais de otimização e engenharia
    │   ├── CHECKLIST_DIAGNOSTICO_SAUDE_SISTEMA.md # Lead magnet técnico de 15 pontos
    │   ├── QUESTIONARIO_REQUISITOS_SISTEMAS_SOB_MEDIDA.md (.pdf) # Questionário de escopo
    │   ├── GUIA_COLABORADOR_CAPTURA_E_REQUISITOS_CLAUDE.md (.pdf) # Discovery com Claude
    │   ├── PLANO_EXPANSAO_NACIONAL_PME.md         # Estratégia de vendas nacional para PMEs
    │   ├── AUDITORIA_CETICA_PLANO_EXPANSAO_NACIONAL_PME.md # Auditoria do plano de expansão
    │   ├── PLANO_ESTRATEGICO_MARKETING_DISTRIBUICAO_B2B.md # Distribuição multicanal
    │   ├── AUDITORIA_CETICA_PLANOS_MARKETING_E_PUBLICIDADE.md # Auditoria de marketing B2B
    │   ├── SCRIPTS_PROSPECCAO_OUTBOUND.md         # Scripts de abordagem para WhatsApp/LinkedIn
    │   ├── PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md # Protocolo de segurança anti-bloqueio
    │   └── PARECER_TECNICO_ARQUITETURA_ENTREGA_SOLO.md # Análise de viabilidade solo
    │
    ├── 03_social_media/                           # [Frente 3] Social Media e Autoridade
    │   ├── README.md                              # Guia de estratégia editorial
    │   ├── PLAYBOOK_LINKEDIN_AUTORIDADE.md        # Framework Dev-to-Founder no LinkedIn
    │   └── PLANO_PUBLICIDADE_E_CAMPANHAS_REDES_SOCIAIS.md # Campanhas e roteiros de vídeos
    │
    ├── 04_rh/                                     # [Frente 4] Recursos Humanos e Avaliação
    │   ├── README.md                              # Guia de recrutamento prático
    │   └── AVALIACAO_TECNICA_DEV.md               # Desafio técnico em Java, Angular e SQL
    │
    ├── 05_administracao/                          # [Frente 5] Administração, FinOps e SLA
    │   ├── README.md                              # Guia de processos administrativos e FinOps
    │   ├── MATRIZ_SEVERIDADE_E_SLA.md             # Tabela oficial de incidentes e SLAs
    │   ├── RUNBOOK_ONBOARDING_NOVO_CLIENTE.md     # Protocolo de assunção de legado em 72h
    │   ├── PLANO_CONTINUIDADE_E_BACKUP_TECNICO.md # Proteção de homem-chave e Disaster Recovery
    │   ├── PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.md (.docx) # Simples Nacional a 6% via Fator R
    │   └── GUIA_INFRAESTRUTURA_EMAILS_CORPORATIVOS.md # Configuração DNS de e-mails
    │
    ├── 06_identidade_visual/                      # [Frente 6] Identidade Visual e Spartan UI
    │   ├── README.md                              # Guia de governança de marca e design
    │   ├── MANUAL_DE_IDENTIDADE_VISUAL_TRD.md     # Manual de marca, cores Pantone e WCAG AAA
    │   ├── PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md  # Padrão Spartan UI oficial para sistemas TRD
    │   ├── CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md # Design system fiduciário Dual-Theme
    │   ├── tokens.json & tokens.css               # Design tokens W3C e variáveis CSS
    │   ├── tailwind.config.js                     # Configuração oficial para Tailwind CSS
    │   ├── LogoTRD.tsx                            # Componente React / Next.js
    │   ├── assinatura_email.html                  # Assinatura de e-mail corporativo
    │   └── papel_timbrado_template.md             # Template de timbrado para laudos
    │
    ├── 07_tributario_rtc/                         # [Frente 7] Motores Oficiais Reforma Tributária
    │   ├── README.md                              # Vinculação estratégica dos motores com a TRD
    │   ├── calculadora-rtc-simples-nacional/      # API REST da RFB para CBS/IBS no Simples
    │   ├── calculadora-rtc-regime-regular/        # API REST da RFB para Regime Regular
    │   └── calculadora-rtc-gerador-danfse/        # Microserviço em Go para DANFSe PDF
    │
    └── openbot_integracao/                        # [Frente 8] Squad OpenBot (17 Agentes de IA)
        ├── README.md                              # Arquitetura de orquestração multiagente
        ├── BLUEPRINT_MAQUINA_EMPRESARIAL_TRD.md   # Arquitetura dos 5 motores e contratos JSON
        ├── ARQUITETURA_MAQUINA_ESTADOS_VETO.md    # Máquina de estados de veto e circuit breaker
        ├── ESPECIFICACAO_SOCIAL_MEDIA_BRASIL.md   # Engenharia de mídia multicanal mobile PME
        ├── PARECER_CETICO_HEADCOUNT_AGENTES.md    # Parecer crítico de headcount e racionalização
        ├── ARQUITETURA_ESTEIRA_AUTOMATIZADA_OPENBOT.md # Arquitetura da esteira de agentes
        ├── MATRIZ_TATICA_FALLBACK_8_NIVEIS.md (.pdf) # Matriz tática de resiliência de LLMs
        ├── MATRIZ_PERSONAS_SKILLS_AGENTES.md      # Matriz de personas e skills dos 17 agentes
        ├── PROTOCOLO_CANONICO_MENSAGERIA.md       # Protocolo canônico de mensageria
        ├── TEMPLATE_NOVO_AGENTE.md                # Template canônico para novos agentes
        ├── agentes/                               # Prompts operacionais dos agentes
        └── skills/                                # Arsenal de 72 skills especializadas
```

---

## 4. DIRETRIZES DE MANUTENÇÃO & INTEGRIDADE

1. **Inviolabilidade de Arquivos:** NENHUM arquivo do repositório pode ser deletado, renomeado ou truncado sem aprovação prévia do Fundador e parecer favorável d'O Cético.
2. **Atualização de Links:** Toda nova adição de documento deve ser imediatamente indexada neste catálogo e no respectivo `README.md` do seu diretório de origem.
3. **Padrão Dual-Format:** Documentos contratuais e propostas devem manter a paridade rigorosa entre as versões editáveis (`.docx`), finais para assinatura (`.pdf`) e versões de visualização no repositório (`.md`).
