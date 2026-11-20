# Operações 01 — Jurídico, Contratos & Governança Fiduciária
### TRD Consultoria e Serviços de TI LTDA
**CNPJ:** 43.065.207/0001-57 | **Sede:** Brasília/DF | **Fundador:** Thiago Rocha Duarte  
*Framework de Blindagem Jurídica para Prestação de Serviços de TI, Desenvolvimento e Sustentação de Missão Crítica*

---

## 1. Visão Geral do Núcleo Jurídico

Este diretório abriga todos os instrumentos contratuais, minutas constitutivas, termos de sigilo (NDA) e acordos de parceria da **TRD Consultoria**. Toda a arquitetura jurídica foi elaborada em estrita conformidade com a legislação brasileira aplicável ao mercado de tecnologia:

- **Lei do Software (Lei nº 9.609/1998):** Propriedade intelectual e transferência de código condicionadas à quitação financeira integral.
- **Código Civil Brasileiro (Lei nº 10.406/2002):** Contratos de prestação de serviços civis PJ, exclusão de vínculo empregatício, regramento de horas não cumulativas e aviso prévio.
- **Lei Geral de Proteção de Dados (Lei nº 13.709/2018 - LGPD):** Delimitação do papel da TRD como **Operadora** e do cliente como **Controlador**, com dever de cooperação e sigilo estrito.
- **Marco Civil da Internet (Lei nº 12.965/2014):** Padrões fiduciários de guarda de registros de conexão e acesso a aplicações (Art. 15).
- **Assinatura Eletrônica (Lei nº 14.063/2020):** Validade jurídica plena de assinaturas digitais avançadas e qualificadas via Gov.br, ZapSign e ClickSign.

---

## 2. Catálogo de Instrumentos Jurídicos

| Instrumento | Formatos | Finalidade / Cláusulas Chave |
| :--- | :---: | :--- |
| **[Contrato de Sustentação & SLA](CONTRATO_SUSTENTACAO_E_SLA.md)** | `.md` · `.docx` · `.pdf` | Contrato continuado para suporte P1-P4, banco de horas técnicas, *Damage Cap* (limite de responsabilidade civil) e retenção de código fonte. |
| **[Contrato de Desenvolvimento de Software](CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md)** | `.md` · `.pdf` | Contrato sob medida para MVPs, hubs de gestão e CRMs. Estabelece entregas por marcos (Milestones), custódia em ambiente Staging e protocolo anti-chargeback. |
| **[Acordo de Confidencialidade (NDA)](ACORDO_CONFIDENCIALIDADE_NDA.md)** | `.md` · `.docx` | Minuta bilateral obrigatória para fase pré-contratual de descoberta de sistemas, diagnósticos de arquitetura e proteção de segredos industriais. |
| **[Acordo de Sócios (Minuta para Vesting)](ACORDO_DE_SOCIOS.md)** | `.md` · `.docx` | Minuta societária contingencial para eventual admissão futura de sócios técnicos ou executivos, com cláusula de Vesting de 4 anos e Cliff de 12 meses. |
| **[Acordo de Parceria Comercial & SDR Freelance](ACORDO_PARCERIA_COMERCIAL_E_REQUISITOS_FREELANCE.md)** | `.md` · `.pdf` | Regramento para agentes externos de prospecção comercial (SDRs/BDRs), comissionamento por fechamento e parâmetros de requisitos técnicos. |
| **[Termo de Homologação e Entrega Definitiva](TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md)** | `.md` | Ateste formal emitido pós-validação em homologação, com quitação de etapas e renúncia expressa a contestação retroativa. |
| **[Termo de Parceria e Comissão de Indicação](TERMO_PARCERIA_E_COMISSAO_INDICACAO.md)** | `.md` | Minuta para canal de agências de marketing, consultorias contábeis e parceiros de indicação (comissão recorrente de 10% a 15%). |
| **[Contrato de Sobreaviso Técnico (Backup PJ)](CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md)** | `.md` | Instrumento para credenciamento de engenheiros PJ de contingência (Disaster Recovery e cobertura do homem-chave). |
| **[Guia de Assinatura Digital Gov.br](GUIA_ASSINATURA_DIGITAL_GOVBR.md)** | `.md` | Manual passo-a-passo para orientar clientes e parceiros a assinar documentos em PDF com certificado digital ICP-Brasil / Gov.br sem custos. |

---

## 3. Diretrizes de Uso Operacional

1. **Pré-Venda e Elicitação:** Antes de receber qualquer acesso a bancos de dados legados ou repositórios privados do prospect, assine o [ACORDO_CONFIDENCIALIDADE_NDA.md](ACORDO_CONFIDENCIALIDADE_NDA.md).
2. **Fechamento Comercial:** Gere a proposta e vincule-a ao [CONTRATO_SUSTENTACAO_E_SLA.md](CONTRATO_SUSTENTACAO_E_SLA.md) ou ao [CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md](CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md).
3. **Assinatura Digital:** Envie o PDF gerado para coleta de assinaturas via [Gov.br Assinador](GUIA_ASSINATURA_DIGITAL_GOVBR.md) ou plataforma homologada (ClickSign/ZapSign).
4. **Entrega de Marcos:** Ao concluir um ciclo ou sprint, colete a assinatura do [TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md](TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md) antes da liberação dos fontes em produção.
