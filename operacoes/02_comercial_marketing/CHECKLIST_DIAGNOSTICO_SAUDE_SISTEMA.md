# CHECKLIST EXECUTIVO: DIAGNÓSTICO DE SAÚDE, RISCO & ESTABILIDADE DE SISTEMAS
### Ferramenta Prática de Autoavaliação para Diretores de TI, COOs e Gerentes de Operações
**TRD Consultoria e Serviços de TI LTDA — Brasília/DF**

> [!TIP]
> **Como Utilizar Este Checklist:** Avalie o sistema corporativo ou aplicação legada da sua empresa atribuindo **1 ponto para cada resposta "SIM"** nas perguntas abaixo. Quanto maior a pontuação de risco, maior a probabilidade de paradas catastróficas, lentidão em fechamentos de mês e passivos trabalhistas/LGPD.

---

## 🛑 BLOCO 1: Dependência de Pessoas & Documentação (Fator Homem-Chave)

- [ ] **1. Dependência Exclusiva de 1 Pessoa:** Existe apenas uma única pessoa (ou desenvolvedor terceirizado) que conhece as regras de negócio e a estrutura do código-fonte do sistema principal?
- [ ] **2. Código Sem Documentação Viva:** Se essa pessoa se ausentar por 30 dias (férias, doença ou desligamento repentino), a empresa é incapaz de realizar deploys ou corrigir falhas críticas com agilidade?
- [ ] **3. Ausência de Repositório Corporativo Próprio:** O código-fonte reside em máquinas locais de desenvolvedores ou em contas pessoais fora da custódia formal da sua empresa?
- [ ] **4. Processo Manual de Deploy:** Cada subida para produção depende de cópia manual de arquivos via FTP/SSH ou scripts sem esteira automatizada de CI/CD?

---

## ⚙️ BLOCO 2: Banco de Dados, Performance & Integridade

- [ ] **5. Lentidão em Horário de Pico:** O sistema apresenta lentidão perceptível ou travamentos nas tabelas durante o horário comercial de maior movimento?
- [ ] **6. Fechamentos Mensais em Risco:** Rotinas de emissão massiva de notas fiscais, sincronização de pedidos de representantes ou relatórios fiscais levam horas ou travam o servidor?
- [ ] **7. Falta de Teste Real de Restauração de Backup:** A empresa realiza backups regulares do banco de dados, mas **nunca realizou uma simulação real de restauração completa** nos últimos 6 meses para testar se os arquivos são válidos?
- [ ] **8. Crescimento Descontrolado de Logs e Tabelas:** O banco de dados nunca passou por rotinas de particionamento, desfragmentação ou reindexação periódica?

---

## 🔒 BLOCO 3: Segurança da Informação, LGPD & Conformidade

- [ ] **9. Senhas e Credenciais Compartilhadas:** Desenvolvedores e prestadores utilizam o usuário `root` ou `admin` com a mesma senha compartilhada em arquivos de texto ou conversas de WhatsApp?
- [ ] **10. Dados Sensíveis Sem Criptografia em Repouso:** Dados pessoais de clientes (CPF, endereços, dados bancários) ficam expostos em texto puro sem adequação à LGPD?
- [ ] **11. Dependências com Vulnerabilidades Conhecidas:** A aplicação utiliza bibliotecas, frameworks (ex: versões antigas de Java, PHP ou Node) com falhas de segurança conhecidas (CVEs) sem patches aplicados?
- [ ] **12. Ausência de Trilha de Auditoria:** É impossível identificar com precisão quem alterou, excluiu ou consultou determinado registro sensível no banco de dados?

---

## 🚀 BLOCO 4: Inteligência Artificial & Arquitetura Moderna

- [ ] **13. Automação Amadora de IA:** A empresa tenta usar IA conectando dados internos diretamente a chatbots públicos sem governança de segurança ou vetor de contexto privado?
- [ ] **14. Ausência de Testes Automatizados de Regressão:** Qualquer correção de bug exige testes manuais extensivos pelo próprio usuário final em produção?
- [ ] **15. Falta de SLA Contratual com Fornecedores:** Se o sistema cair às 10h da manhã de uma terça-feira, não existe nenhum contrato formal que obrigue um tempo máximo de resposta garantido?

---

## 📊 Tabela de Interpretação do Score de Risco

| Pontos "SIM" | Nível de Risco | Diagnóstico Operacional | Ação Recomendada |
| :---: | :---: | :--- | :--- |
| **0 a 3** | 🟢 **Baixo Risco** | Operação saudável com boa maturidade técnica e governança. | Manter rotinas preventivas e auditoria trimestral. |
| **4 a 7** | 🟡 **Risco Moderado** | Vulnerabilidades latentes. Uma falha de banco ou saída de dev gerará parada operacional imprevista. | **Auditoria de Arquitetura (Discovery de 5 dias)** para sanar pontos críticos antes de quebrar. |
| **8 a 15** | 🔴 **Risco Crítico** | **Alerta Vermelho.** O sistema opera no limite técnico. Alta probabilidade de parada severa ou perda de faturamento. | **Intervenção Imediata de Engenharia Sênior e Contrato de Sustentação com SLA.** |

---

> 🎯 **Precisa de Apoio Técnico para Eliminar Esses Riscos?**  
> A **TRD Consultoria e Serviços de TI LTDA** realiza a **Auditoria de Arquitetura & Diagnóstico Técnico (Discovery)** em até 5 dias úteis, entregando um laudo executivo completo com plano de remediação.  
> Contato: `contato@trdconsultoria.com.br` | WhatsApp: **+55 (61) 99952-6374** | Brasília - DF
