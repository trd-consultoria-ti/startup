# Central de Operações da Empresa (AGY & OpenBot Operations Hub)
### TRD Consultoria e Serviços de TI LTDA
*Framework Operacional, Jurídico, Comercial, FinOps, Design System e Motores Tributários da RTC*

---

> 📖 **Catálogo Completo:** Para um índice analítico com trilhas de navegação personalizadas por perfil (Comercial, Jurídico, Operações, FinOps, Frontend e IA), consulte o [**CATÁLOGO_ARTEFATOS.md**](../CATALOGO_ARTEFATOS.md) na raiz do repositório.

---

## Estrutura Operacional Consolidada (8 Frentes)

```
operacoes/
├── README.md                                       # Este guia consolidado da esteira operacional
│
├── 01_juridico/                                    # [Frente 1] Jurídico, Contratos e Governança Societária
│   ├── README.md                                   # Guia do núcleo jurídico e legislação brasileira
│   ├── CONTRATO_SUSTENTACAO_E_SLA.md (.docx / .pdf) # Contrato continuado com Damage Cap e retenção de IP
│   ├── CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md (.pdf) # Contrato de criação de software por marcos
│   ├── ACORDO_CONFIDENCIALIDADE_NDA.md (.docx)     # Acordo bilateral de sigilo (LGPD compliant)
│   ├── ACORDO_DE_SOCIOS.md (.docx)                 # Minuta contingencial com Vesting (4 anos)
│   ├── ACORDO_PARCERIA_COMERCIAL_E_REQUISITOS_FREELANCE.md (.pdf) # Parceria com SDRs
│   ├── CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md # Engenheiro backup sobreaviso PJ
│   ├── TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md   # Ateste formal com renúncia a contestação
│   ├── TERMO_PARCERIA_E_COMISSAO_INDICACAO.md      # Parcerias com agências (10-15% comissão)
│   └── GUIA_ASSINATURA_DIGITAL_GOVBR.md            # Passo a passo para assinatura digital Gov.br
│
├── 02_comercial_marketing/                         # [Frente 2] Comercial, Vendas e Elicitação
│   ├── README.md                                   # Guia da esteira comercial e prospecção
│   ├── PROPOSTA_COMERCIAL_EXECUTIVA.md (.docx / .pdf) # Proposta oficial com pacotes de R$ 3,8k a R$ 8,5k
│   ├── ONE_PAGER_INSTITUCIONAL_TRD.md (.docx / .pdf) # Folheto executivo de 1 página (WhatsApp/LinkedIn)
│   ├── ANEXO_I_FICHA_TECNICA_STARTER_9800.md       # Ficha técnica do MVP Starter em 20 dias
│   ├── CASES_DE_SUCESSO_E_AUTORIDADE.md            # Casos reais de otimização SQL, Java e LGPD
│   ├── CHECKLIST_DIAGNOSTICO_SAUDE_SISTEMA.md      # Lead magnet técnico: 15 pontos de risco
│   ├── QUESTIONARIO_REQUISITOS_SISTEMAS_SOB_MEDIDA.md (.pdf) # Questionário de escopo técnico
│   ├── GUIA_COLABORADOR_CAPTURA_E_REQUISITOS_CLAUDE.md (.pdf) # Elicitação de requisitos com Claude
│   ├── PLANO_EXPANSAO_NACIONAL_PME.md              # Estratégia mestre de vendas para PMEs
│   ├── AUDITORIA_CETICA_PLANO_EXPANSAO_NACIONAL_PME.md # Auditoria do plano de expansão
│   ├── PLANO_ESTRATEGICO_MARKETING_DISTRIBUICAO_B2B.md # Distribuição multicanal B2B
│   ├── AUDITORIA_CETICA_PLANOS_MARKETING_E_PUBLICIDADE.md # Auditoria de marketing e budget
│   ├── SCRIPTS_PROSPECCAO_OUTBOUND.md              # 5 scripts de abordagem comercial
│   ├── PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md    # Protocolo de permission marketing anti-bloqueio
│   └── PARECER_TECNICO_ARQUITETURA_ENTREGA_SOLO.md # Viabilidade de engenharia para entrega solo
│
├── 03_social_media/                                # [Frente 3] Social Media e Autoridade Orgânica
│   ├── README.md                                   # Guia editorial e posicionamento Dev-to-Founder
│   ├── PLAYBOOK_LINKEDIN_AUTORIDADE.md             # Estratégia editorial e templates de posts técnicos
│   └── PLANO_PUBLICIDADE_E_CAMPANHAS_REDES_SOCIAIS.md # Campanhas estruturadas e roteiros de vídeos
│
├── 04_rh/                                          # [Frente 4] Recursos Humanos e Avaliação Técnica
│   ├── README.md                                   # Guia de recrutamento prático de engenharia
│   └── AVALIACAO_TECNICA_DEV.md                    # Desafio técnico em Java, Angular e SQL
│
├── 05_administracao/                               # [Frente 5] Administração, FinOps, SLA e Continuidade
│   ├── README.md                                   # Guia de processos administrativos e FinOps
│   ├── MATRIZ_SEVERIDADE_E_SLA.md                  # Protocolo de incidentes P1 a P4 e gestão de chamados
│   ├── RUNBOOK_ONBOARDING_NOVO_CLIENTE.md          # Protocolo padronizado de assunção em 72 horas
│   ├── PLANO_CONTINUIDADE_E_BACKUP_TECNICO.md      # Protocolo homem-chave e cofre Bitwarden
│   ├── PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.md (.docx) # Manual CFO: Simples Nacional a 6% via Fator R
│   └── GUIA_INFRAESTRUTURA_EMAILS_CORPORATIVOS.md  # Configuração DNS de e-mails corporativos
│
├── 06_identidade_visual/                           # [Frente 6] Identidade Visual, Spartan UI e Tokens
│   ├── README.md                                   # Guia de governança de marca e design system
│   ├── MANUAL_DE_IDENTIDADE_VISUAL_TRD.md          # Manual de marca, cores Pantone e WCAG AAA
│   ├── PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md       # Diretriz oficial Spartan UI para sistemas TRD
│   ├── CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md # Design system fiduciário B2B Dual-Theme
│   ├── tokens.json & tokens.css                    # Design tokens oficiais W3C e variáveis CSS
│   ├── tailwind.config.js                          # Configuração oficial para Tailwind CSS
│   ├── LogoTRD.tsx                                 # Componente oficial React / Next.js
│   ├── assinatura_email.html                       # Assinatura de e-mail corporativo em HTML
│   └── papel_timbrado_template.md                  # Template de timbrado para propostas e laudos
│
├── 07_tributario_rtc/                              # [Frente 7] Motores Oficiais Reforma Tributária (RFB)
│   ├── README.md                                   # Matriz estratégica de vinculação com Fator R e FinOps
│   ├── calculadora-rtc-simples-nacional/           # API REST oficial da RFB para CBS/IBS no Simples
│   ├── calculadora-rtc-regime-regular/             # API REST oficial da RFB para Regime Regular
│   └── calculadora-rtc-gerador-danfse/             # Microserviço em Go da RFB para DANFSe em PDF
│
└── openbot_integracao/                             # [Frente 8] Squad OpenBot (17 Agentes de IA)
    ├── README.md                                   # Arquitetura de orquestração multiagente
    ├── BLUEPRINT_MAQUINA_EMPRESARIAL_TRD.md        # Arquitetura dos 5 motores e contratos JSON
    ├── ARQUITETURA_MAQUINA_ESTADOS_VETO.md         # Máquina de estados de veto e circuit breaker
    ├── ESPECIFICACAO_SOCIAL_MEDIA_BRASIL.md        # Engenharia de mídia multicanal mobile PME
    ├── PARECER_CETICO_HEADCOUNT_AGENTES.md         # Parecer crítico de headcount e racionalização
    ├── ARQUITETURA_ESTEIRA_AUTOMATIZADA_OPENBOT.md  # Arquitetura da esteira automatizada de agentes
    ├── MATRIZ_TATICA_FALLBACK_8_NIVEIS.md (.pdf)  # Matriz tática de resiliência de LLMs
    ├── MATRIZ_PERSONAS_SKILLS_AGENTES.md           # Matriz de personas e skills dos 17 agentes
    ├── PROTOCOLO_CANONICO_MENSAGERIA.md            # Protocolo canônico de mensageria token-optimized
    ├── TEMPLATE_NOVO_AGENTE.md                     # Template canônico para novos agentes
    ├── agentes/                                    # Prompts operacionais e regras de cada agente
    └── skills/                                     # Arsenal de 72 skills especializadas dos 17 agentes
```

---

## Como Operar Esta Estrutura

1. **Para Fechar um Novo Cliente:**
   * Preencha a [PROPOSTA_COMERCIAL_EXECUTIVA.md](02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.md) e exporte para PDF.
   * Ao aceitar, gere o [CONTRATO_SUSTENTACAO_E_SLA.md](01_juridico/CONTRATO_SUSTENTACAO_E_SLA.md) assinado via ClickSign/ZapSign/Gov.br.
2. **Para Prospectar Novos Clientes no DF e Brasil:**
   * Utilize os templates em [SCRIPTS_PROSPECCAO_OUTBOUND.md](02_comercial_marketing/SCRIPTS_PROSPECCAO_OUTBOUND.md) e o [PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md](02_comercial_marketing/PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md).
3. **Para Gerar Autoridade Orgânica:**
   * Aplique os modelos de [PLAYBOOK_LINKEDIN_AUTORIDADE.md](03_social_media/PLAYBOOK_LINKEDIN_AUTORIDADE.md).
4. **Para Atender e Controlar Chamados Técnicos:**
   * Siga o protocolo rigoroso de [MATRIZ_SEVERIDADE_E_SLA.md](05_administracao/MATRIZ_SEVERIDADE_E_SLA.md).
5. **Para Construir Telas no Padrão Institucional:**
   * Siga as diretrizes de [PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md](06_identidade_visual/PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md) e [CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md](06_identidade_visual/CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md).
6. **Para Simular Impactos da Reforma Tributária (RTC):**
   * Consulte a documentação e execute os motores em [operacoes/07_tributario_rtc/README.md](07_tributario_rtc/README.md).
