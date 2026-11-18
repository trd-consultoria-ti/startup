# Central de Operações da Empresa (AGY Operations Hub)

Este diretório contém a esteira operacional completa, os contratos, os scripts de vendas e os playbooks de execução da nossa empresa de desenvolvimento, sustentação e IA.

---

## Estrutura Operacional

```
operacoes/
├── 01_juridico/
│   ├── CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md # Contrato com custódia Staging, SPOC e cláusula anti-chargeback
│   ├── TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md       # Ateste formal Gov.br com renúncia expressa a contestação/chargeback
│   ├── CONTRATO_SUSTENTACAO_E_SLA.md (.docx)   # Minuta jurídica com Damage Cap e SLA blindado
│   ├── ACORDO_CONFIDENCIALIDADE_NDA.md (.docx) # Acordo de sigilo e proteção de código/dados
│   ├── ACORDO_DE_SOCIOS.md (.docx)             # Minuta contingencial para admissão futura de sócio (Vesting de 4 anos)
│   ├── TERMO_PARCERIA_E_COMISSAO_INDICACAO.md  # Contrato de canal de agências/white-label (comissão 10-15%)
│   └── CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md # Contrato PJ de engenheiro de sobreaviso (Backup Técnico)
├── 02_comercial_marketing/
│   ├── PLANO_EXPANSAO_NACIONAL_PME.md          # Estratégia mestre de vendas para PMEs no Brasil (Preços, Polos e Scripts)
│   ├── AUDITORIA_CETICA_PLANO_EXPANSAO_NACIONAL_PME.md # Stress-test e 5 blindagens inegociáveis para PMEs
│   ├── PARECER_TECNICO_ARQUITETURA_ENTREGA_SOLO.md     # Validação de viabilidade técnica solo (20 dias, Boilerplate 80/20)
│   ├── ANEXO_I_FICHA_TECNICA_STARTER_9800.md   # Ficha técnica imutável do MVP Starter (Anti-Scope Creep)
│   ├── PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md # Protocolo de permission marketing de 40s e chip secundário
│   ├── PLANO_ESTRATEGICO_MARKETING_DISTRIBUICAO_B2B.md # Distribuição de canais B2B, Google Ads, LinkedIn e Budget
│   ├── AUDITORIA_CETICA_PLANOS_MARKETING_E_PUBLICIDADE.md # Stress-test de riscos, blindagem operacional e travas de SLA
│   ├── PROPOSTA_COMERCIAL_EXECUTIVA.md (.docx) # Proposta oficial com pacotes de R$ 3,8k a R$ 8,5k
│   ├── ONE_PAGER_INSTITUCIONAL_TRD.md (.docx)  # Folheto executivo de 1 página para envio no WhatsApp/LinkedIn
│   ├── CHECKLIST_DIAGNOSTICO_SAUDE_SISTEMA.md  # Lead magnet técnico: 15 pontos de risco para diretores de TI
│   ├── CASES_DE_SUCESSO_E_AUTORIDADE.md        # 3 estudos de caso estruturais (SQL, Legado Java e LGPD)
│   └── SCRIPTS_PROSPECCAO_OUTBOUND.md          # Scripts de WhatsApp, LinkedIn e abordagem de agências
├── 03_social_media/
│   ├── PLANO_PUBLICIDADE_E_CAMPANHAS_REDES_SOCIAIS.md # 4 conceitos de campanhas, carrossel e roteiro de vídeo
│   └── PLAYBOOK_LINKEDIN_AUTORIDADE.md         # Estratégia editorial e templates de posts técnicos
├── 04_rh/
│   └── AVALIACAO_TECNICA_DEV.md                # Desafio prático para estagiários/suporte (Java/Angular/SQL)
├── 05_administracao/
│   ├── MATRIZ_SEVERIDADE_E_SLA.md              # Protocolo de atendimento P1/P2/P3 e gestão de chamados
│   ├── RUNBOOK_ONBOARDING_NOVO_CLIENTE.md      # Protocolo padronizado de assunção de sistemas em 72 horas
│   ├── PLANO_CONTINUIDADE_E_BACKUP_TECNICO.md  # Protocolo homem-chave, cofre Bitwarden e Disaster Recovery
│   └── PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.md (.docx) # Manual CFO: Simples Nacional a 6%, Fator R e Reservas
├── 06_identidade_visual/
│   ├── MANUAL_DE_IDENTIDADE_VISUAL_TRD.md      # Manual de governança, Pantone, Rich Black e WCAG AAA
│   ├── tokens.json / tokens.css                # Design tokens oficiais de cores e tipografia
│   ├── LogoTRD.tsx                             # Componente oficial React / Next.js
│   └── papel_timbrado_template.md              # Template de timbrado para propostas e laudos
└── openbot_integracao/                         # Integração e Robôs Especialistas para o OpenBot
    ├── README.md                               # Guia de implantação e fluxo de orquestração
    └── agentes/                                # Perfis prontos: Comercial, Jurídico, SLA, FinOps, Marketing, RH
```

---

## Como Operar Esta Estrutura no Antigravity (AGY)

1. **Para Fechar um Novo Cliente:**
   * Preencha a [PROPOSTA_COMERCIAL_EXECUTIVA.md](02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.md) e exporte para PDF.
   * Ao aceitar, gere o [CONTRATO_SUSTENTACAO_E_SLA.md](01_juridico/CONTRATO_SUSTENTACAO_E_SLA.md) assinado via ClickSign/ZapSign/Gov.br.
2. **Para Prospectar Novos Clientes no DF e Brasil:**
   * Utilize os templates em [SCRIPTS_PROSPECCAO_OUTBOUND.md](02_comercial_marketing/SCRIPTS_PROSPECCAO_OUTBOUND.md).
3. **Para Gerar Autoridade Orgânica:**
   * Aplique os modelos de [PLAYBOOK_LINKEDIN_AUTORIDADE.md](03_social_media/PLAYBOOK_LINKEDIN_AUTORIDADE.md).
4. **Para Atender e Controlar Chamados Técnicos:**
   * Siga o protocolo rigoroso de [MATRIZ_SEVERIDADE_E_SLA.md](05_administracao/MATRIZ_SEVERIDADE_E_SLA.md).
