# Operações 05 — Administração, FinOps, SLA & Continuidade de Negócios
### TRD Consultoria e Serviços de TI LTDA
*Gestão de Incidentes de Missão Crítica, Onboarding de Clientes, Engenharia Tributária e Disaster Recovery*

---

## 1. Visão Geral da Frente Administrativa & FinOps

Este diretório centraliza os processos vitais que sustentam a operação contínua e a saúde financeira da **TRD Consultoria**:

- **Gestão de Incidentes e Sustentação:** Classificação de chamados de P1 (parada crítica) a P4 (dúvidas) com metas de SLA contratual e tempos de resposta rigorosos.
- **Onboarding Acelerado em 72 Horas:** Metodologia estruturada para assumir o controle técnico de sistemas legados de novos clientes sem interrupção de serviço.
- **Continuidade & Mitigação do Homem-Chave:** Protocolos de contingência, cofre de senhas centralizado em Bitwarden e planos de Disaster Recovery.
- **Engenharia Tributária & FinOps:** Enquadramento contábil no Simples Nacional sob a alíquota otimizada de 6% no Anexo III através da blindagem matemática do Fator R (folha de salários $\ge$ 28% da receita bruta).

---

## 2. Catálogo de Runbooks e Normas Administrativas

| Instrumento | Formatos | Propósito e Responsabilidade |
| :--- | :---: | :--- |
| **[Matriz de Severidade e SLA](MATRIZ_SEVERIDADE_E_SLA.md)** | `.md` | Tabela canônica de classificação de incidentes (P1 a P4), tempos de resposta de 1h a 24h e fluxo de escalonamento técnico. |
| **[Runbook de Onboarding de Novo Cliente](RUNBOOK_ONBOARDING_NOVO_CLIENTE.md)** | `.md` | Procedimento operacional padrão de assunção de ambientes legados em 72h (inventário de credenciais, espelhamento de banco, pipeline CI/CD). |
| **[Plano de Continuidade & Backup Técnico](PLANO_CONTINUIDADE_E_BACKUP_TECNICO.md)** | `.md` | Protocolo para mitigação de risco do homem-chave, documentação de custódia no cofre Bitwarden e acionamento de engenheiro backup PJ. |
| **[Planejamento Tributário & Financeiro](PLANEJAMENTO_TRIBUTARIO_E_FINANCEIRO.md)** | `.md` · `.docx` | Manual executivo de FinOps: parametrização do Simples Nacional a 6%, monitoramento do Fator R, pró-labore e provisões de caixa. |
| **[Guia de Infraestrutura de E-mails Corporativos](GUIA_INFRAESTRUTURA_EMAILS_CORPORATIVOS.md)** | `.md` | Especificação técnica de configuração DNS (MX, SPF, DKIM, DMARC) para os domínios corporativos `@trdconsultoria.com.br`. |

---

## 3. Conexão com os Motores Oficiais da Reforma Tributária (RTC)

A governança financeira e tributária deste módulo se conecta diretamente aos motores oficiais da Receita Federal mantidos em:
- [operacoes/07_tributario_rtc/](../07_tributario_rtc/README.md) — APIs oficiais da RFB para apuração da CBS/IBS (Simples Nacional e Regime Regular) e microserviço em Go para emissão fiduciária de DANFSe em PDF.
