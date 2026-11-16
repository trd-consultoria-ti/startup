# PLANO DE CONTINUIDADE OPERACIONAL & PROTOCOLO PESSOA-CHAVE (DISASTER RECOVERY)
### Mitigação do Risco Operacional "Bus Factor = 1" & Blindagem de Continuidade de Negócios
**TRD Consultoria e Serviços de TI LTDA — Brasília/DF**

---

## 1. Objetivo & Princípio de Blindagem
Em uma boutique de engenharia de software operada originariamente por fundador solo, o maior risco percebido por clientes corporativos e auditorias é a **indisponibilidade repentina do profissional-chave (*Key-Person Risk*)** decorrente de:
* Problemas de saúde, hospitalização ou emergências familiares;
* Viagens prolongadas ou falhas catastróficas de hardware pessoal;
* Picos simultâneos de incidentes críticos P1 em múltiplos clientes.

Este documento estabelece as políticas de custódia de credenciais, redundância de acessos e acionamento de retaguarda técnica para assegurar que **nenhum cliente da TRD fique desassistido em nenhuma circunstância.**

---

## 2. A Custódia de Senhas & Cofre Criptografado de Emergência (Vault)

### 2.1. O Princípio do Cofre com Acesso de Emergência
Nenhuma senha, chave SSH, token de API ou credencial de produção de cliente fica armazenada em arquivos de texto locais, blocos de notas ou mensagens.

* **Ferramenta Institucional:** Bitwarden Enterprise ou 1Password Teams corporativo com criptografia de ponta a ponta (AES-256 e Zero-Knowledge).
* **Estrutura de Pastas por Cliente:**
  ```
  [COFRE-TRD]
  ├── Cliente_A/
  │   ├── Acessos_VPN_e_Bastion
  │   ├── Credenciais_Banco_Dados_RO_e_RW
  │   ├── Repositorios_Git_e_CI_CD
  │   └── Contatos_Emergenciais_Cliente (Diretor, Tech Lead)
  ├── Cliente_B/
  └── Chaves_Mestras_TRD/
  ```
* **Mecanismo de Liberação de Emergência (*Emergency Access*):**
  * Configurado o recurso nativo de "Emergency Access" do Bitwarden/1Password, outorgando permissão a 1 contato de confiança jurídica/familiar do fundador para liberar o cofre caso o fundador fique incomunicável por mais de **48 horas**.
  * Esse acesso permite que o engenheiro parceiro cadastrado acesse imediatamente os runbooks e credenciais necessárias para manter os sistemas dos clientes no ar.

---

## 3. Protocolo de Engenheiro Parceiro de Sobreaviso (On-Call Backup)

### 3.1. Estrutura da Retaguarda Técnica
A TRD mantém previamente homologado e contratado sob Termo de Parceria PJ e NDA rigoroso ([CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md](../01_juridico/CONTRATO_PRESTACAO_SERVICOS_PARCEIRO_SOBREAVISO.md)) **1 a 2 engenheiros de software seniores de total confiança pessoal e técnica do fundador**.

### 3.2. Níveis de Acionamento do Backup:

| Nível de Gatilho | Situação Operacional | Ação Imediata da TRD | Remuneração do Parceiro |
| :---: | :--- | :--- | :--- |
| **Nível Verde (Normal)** | Operação regular tocada 100% pelo fundador Thiago. | Parceiro em stand-by passivo; sem custo fixo. | R$ 0,00 |
| **Nível Amarelo (Sobrecarga)** | Volume de chamados P2/P3 ou sprints simultâneas acima de 80% da capacidade mensal. | Thiago aciona o parceiro para absorver tarefas de desenvolvimento/sustentação secundárias. | Valor-hora fechado (R$ 100 a R$ 120/h técnica). |
| **Nível Vermelho (Indisponibilidade)** | Fundador incapacitado temporariamente (doença, cirurgia, força maior). | Contato de confiança aciona o protocolo de contingência; parceiro assume o plantão de SLA P1. | Remuneração proporcional às horas de plantão e intervenção. |

---

## 4. Esteira de Deploys Reprodutível & Infraestrutura Desacoplada

Para que qualquer incidente possa ser resolvido sem depender da máquina física do fundador:
1. **Ambientes Isolados em Contêineres:** Todo projeto mantido possui `Dockerfile` e `docker-compose.yml` versionado no repositório do cliente, permitindo subir o ambiente local de testes em menos de 15 minutos em qualquer computador.
2. **Esteiras de CI/CD (GitHub Actions / GitLab CI):** Nenhum deploy é feito via "arrastar e soltar" em FTP. Todos os deploys em produção passam por pipelines de validação automatizada que exigem build verde antes da publicação.
3. **Runbooks de Sustentação por Cliente:** Cada cliente possui um arquivo privado `RUNBOOK_CLIENTE.md` documentando exatamente como reiniciar serviços, rotacionar certificados e rodar migrações de banco.

---


---

## 4. Política de Salvaguarda de Dados Técnicos e Código-Fonte (RPO & RTO)

Além da continuidade humana e de credenciais, a TRD adota uma política rigorosa de salvaguarda para os repositórios de código e artefatos de infraestrutura geridos para os clientes:

| Métrica | Objetivo Operacional | Implementação Prática |
| :--- | :---: | :--- |
| **RPO (Recovery Point Objective)** | **≤ 24 horas** | Todos os commits e branches de sustentação são espelhados diariamente em repositório remoto redundante. |
| **RTO (Recovery Time Objective)** | **≤ 4 horas úteis** | Tempo máximo para restabelecer a esteira de build e deploy local a partir de backup versionado. |
| **Regra 3-2-1 de Backup de Código** | **Ativa** | 3 cópias do repositório (Origem do cliente, Espelho TRD criptografado e Clone local do desenvolvedor), em 2 mídias distintas, com 1 cópia externa em nuvem isolada. |
| **Testes de Restauração** | **Trimestral** | Simulação periódica de clonagem e compilação limpa a partir dos repositórios de salvaguarda. |

## 5. Como Apresentar Este Plano a Clientes Corporativos
Ao apresentar este Plano de Continuidade durante reuniões de vendas e anexá-lo nas propostas comerciais, a objeção do *homem-chave* é transformada em uma grande demonstração de **profissionalismo e governança**:

> *"Diferente de freelancers que não possuem nenhum plano de contingência se algo acontecer, a TRD opera com governança corporativa de cofre criptografado, runbooks padronizados e esteira de parceiros de sobreaviso formalizados sob contrato e NDA."*
