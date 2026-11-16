# INSTRUMENTO PARTICULAR DE PRESTAÇÃO DE SERVIÇOS DE SUSTENTAÇÃO, MANUTENÇÃO DE SOFTWARE E ACORDO DE NÍVEL DE SERVIÇO (SLA)

Pelo presente instrumento particular, de um lado:

**CONTRATANTE:**
* **Razão Social / Nome:** [NOME DA EMPRESA CLIENTE]
* **CNPJ / CPF:** [00.000.000/0000-00]
* **Endereço:** [Endereço completo, Cidade - UF, CEP]
* **Representante Legal:** [Nome completo do representante], [Nacionalidade], [Estado Civil], portador do RG nº [0000000] e CPF nº [000.000.000-00], doravante denominada simplesmente **CONTRATANTE**;

E, de outro lado:

**CONTRATADA:**
* **Razão Social:** **TRD Consultoria e Serviços de TI LTDA**
* **Nome Fantasia:** TRD Consultoria
* **CNPJ:** **[00.000.000/0001-00 - Em Constituição]**
* **Sede:** Brasília - DF
* **Representada por:** Thiago Rocha Duarte, brasileiro, engenheiro de software, na qualidade de Sócio Administrador, doravante denominada simplesmente **CONTRATADA**;

Ambas doravante denominadas conjuntamente como **PARTES** e individualmente como **PARTE**.

Têm entre si, justo e contratado, o presente Contrato, mediante as seguintes cláusulas e condições:

---

### CLÁUSULA PRIMEIRA – DO OBJETO
1.1. O presente Contrato tem por objeto a prestação contínua de serviços técnicos especializados de **Sustentação, Manutenção Preventiva, Corretiva e Evolutiva** dos sistemas, bancos de dados e aplicações de propriedade ou posse da **CONTRATANTE**, conforme detalhado no **Anexo I (Escopo e Plano Contratado)**.  
1.2. Os serviços compreendem:
* **Manutenção Preventiva:** Monitoramento de estabilidade, verificações de integridade de banco de dados, atualização de dependências e aplicação de patches de segurança.
* **Manutenção Corretiva:** Diagnóstico e correção de falhas de código (*bugs*), travamentos, lentidões e erros de execução em ambiente de produção.
* **Manutenção Evolutiva:** Pequenas melhorias, otimizações de consultas (queries), alterações de regras de negócio e novas rotinas delimitadas pelo banco de horas mensal contratado.

---

### CLÁUSULA SEGUNDA – DO BANCO DE HORAS E REGIME DE ATENDIMENTO
2.1. A **CONTRATADA** disponibilizará mensalmente à **CONTRATANTE** o banco de horas técnicas especializadas correspondente ao plano formalizado no **Anexo I** deste instrumento:
* **Plano Essencial:** 20 (vinte) horas técnicas mensais;
* **Plano Profissional:** 35 (trinta e cinco) horas técnicas mensais;
* **Plano Enterprise:** 60 (sessenta) horas técnicas mensais.  
2.2. **Não Cumulatividade:** As horas contratadas e não utilizadas dentro do mês de vigência **não serão acumuladas** para os meses subsequentes, remunerando a reserva técnica e a disponibilidade contínua do time da **CONTRATADA**.  
2.3. **Horas Excedentes:** Caso a **CONTRATANTE** necessite de volume superior ao banco de horas pactuado, as horas adicionais serão faturadas pelo valor previsto para o plano no Anexo I (R$ 160,00 a R$ 200,00 por hora técnica), mediante aprovação prévia e por escrito (inclusive por e-mail ou WhatsApp oficial) da **CONTRATANTE**.  
2.4. O atendimento regular será realizado em dias úteis, das **08:30 às 18:30 (horário de Brasília)**.

---

### CLÁUSULA TERCEIRA – DO ACORDO DE NÍVEL DE SERVIÇO (SLA)
3.1. A **CONTRATADA** compromete-se a cumprir os seguintes tempos máximos de **Primeira Resposta** e **Início de Atuação**, contados a partir da abertura formal do chamado:

| Severidade | Definição do Incidente | Tempo de Resposta (Horário Comercial) | Início de Atuação |
| :--- | :--- | :---: | :---: |
| **P1 - Crítica** | Sistema totalmente fora do ar, indisponibilidade de faturamento ou perda iminente de dados. | **Até 2h a 4h** *(conforme plano)* | Imediato |
| **P2 - Alta** | Falha grave que prejudica operações essenciais, sem alternativa de contorno imediata. | **Até 4 horas** | Até 6 horas |
| **P3 - Média** | Falha em funcionalidade parcial, lentidão pontual ou demanda com alternativa de contorno. | **Até 8 horas** | Até 24 horas |
| **P4 - Baixa / Evolutiva** | Dúvidas, pequenos ajustes cosméticos ou melhorias de rotina. | **Até 12 horas** | Agendada no backlog |

3.2. **Sobreaviso e Atendimento Fora do Horário Comercial:** Os chamados de emergência de Severidade P1 fora do horário comercial (noites ou fins de semana/feriados) deverão ser acionados via canal de sobreaviso formal, remunerados com o valor adicional de **R$ 250,00 (duzentos e cinquenta reais) por hora técnica**.  
3.3. **Penalidade Exclusiva por Descumprimento de SLA:** Eventual descumprimento injustificado dos prazos estipulados para chamados de Severidade P1 ou P2 conferirá à CONTRATANTE exclusivamente o direito a um desconto financeiro de 5% (cinco por cento) sobre a mensalidade vincenda do mês subsequente por ocorrência, limitado ao teto máximo de 10% (dez por cento) do valor mensal. O descumprimento pontual de SLA não ensejará rescisão automática nem constituirá fundamento para pleito de perdas e danos ou indenizações materiais.

---

### CLÁUSULA QUARTA – DOS VALORES E CONDIÇÕES DE PAGAMENTO
4.1. Pela prestação dos serviços objeto deste contrato, a **CONTRATANTE** pagará à **CONTRATADA** o valor fixo mensal pactuado no Anexo I.  
4.2. O pagamento será efetuado até o dia **05 ou 10** de cada mês subsequente ao vencido, mediante emissão prévia de Nota Fiscal de Serviços Eletrônica (NFS-e) e boleto bancário ou chave PIX emitida pela **CONTRATADA**.  
4.3. Em caso de inadimplemento, incidirá multa moratória de 2% (dois por cento) sobre o valor em atraso, acrescida de juros de mora de 1% (um por cento) ao mês e correção monetária pelo IPCA/IBGE.  
4.4. O atraso superior a 15 (quinze) dias facultará à **CONTRATADA** suspender a execução dos serviços de suporte até a regularização do débito.

---

### CLÁUSULA QUINTA – DA PROPRIEDADE INTELECTUAL
5.1. Todo o código-fonte, scripts de aplicação, documentações e customizações desenvolvidos especificamente para atender às regras de negócio da **CONTRATANTE** em decorrência deste contrato pertencerão exclusivamente à **CONTRATANTE** após a respectiva quitação financeira.  
5.2. A **CONTRATADA** resguarda a titularidade exclusiva sobre suas ferramentas pré-existentes, bibliotecas proprietárias, frameworks internos, métodos de engenharia, prompts de Inteligência Artificial e rotinas-base desenvolvidos de forma independente, outorgando à **CONTRATANTE** apenas uma licença de uso perpétua, irrevogável e não-exclusiva para a operação de seus sistemas.

---

### CLÁUSULA SEXTA – DA PROTEÇÃO DE DADOS (LGPD), SUBPROCESSADORES E CONFIDENCIALIDADE
6.1. As **PARTES** comprometem-se a cumprir integralmente a Lei nº 13.709/2018 (Lei Geral de Proteção de Dados - LGPD).  
6.2. A **CONTRATADA** atuará na condição de **Operadora de Dados Pessoais**, tratando apenas as informações estritamente necessárias para a manutenção e suporte dos sistemas da **CONTRATANTE** (Controladora).  
6.3. **Subprocessadores Tecnológicos Autorizados:** A CONTRATANTE expressamente autoriza a CONTRATADA a utilizar ferramentas corporativas de monitoramento e telemetria (ex: Sentry, Uptime Kuma) e provedores de infraestrutura de desenvolvimento assistido por Inteligência Artificial (ex: Cursor, Anthropic Claude, OpenAI) estritamente para auditoria de código, detecção de falhas e compilação técnica. A CONTRATADA obriga-se a:  
a) Não submeter dados pessoais não-anonimizados de usuários finais da CONTRATANTE às referidas ferramentas de IA;  
b) Utilizar planos corporativos ou configurações com política ativa de não-retenção (*Zero Data Retention*) para treinamento de modelos públicos;  
c) Responder pela segurança e sigilo das ferramentas nos termos do Art. 39 da LGPD.  
6.4. **Gestão de Incidentes de Segurança:** Em caso de incidente comprovado de segurança que acarrete risco ou dano relevante aos titulares de dados pessoais sob custódia da CONTRATADA, esta notificará a CONTRATANTE em até 48 (quarenta e oito) horas úteis após a confirmação do evento.  
6.5. Ambas as partes obrigam-se a manter absoluto sigilo sobre códigos-fontes, segredos de negócio, dados financeiros e credenciais de acesso, sob pena de indenização por perdas e danos.

---

### CLÁUSULA SÉTIMA – DA VIGÊNCIA, RESCISÃO E TRANSIÇÃO OPERACIONAL
7.1. O presente contrato tem vigência de **12 (doze) meses**, iniciando na data de sua assinatura e renovando-se automaticamente por períodos iguais caso nenhuma das partes se manifeste formalmente em contrário com antecedência de 30 dias.  
7.2. Qualquer das partes poderá rescindir o presente contrato imotivadamente mediante aviso prévio por escrito com antecedência mínima de **30 (trinta) dias**, sem incidência de multa após decorridos os primeiros 90 (noventa) dias de vigência.  
7.3. O descumprimento reiterado e injustificado de obrigações essenciais ensejará a rescisão motivada, após notificação formal com prazo de 15 (quinze) dias para saneamento da falha.  
7.4. **Transição Operacional & Eliminação de Dados (Art. 16 da LGPD):** Encerrado o presente contrato por qualquer motivo, a CONTRATADA compromete-se a:  
a) Devolver ou revogar todos os acessos a servidores, repositórios, bancos de dados e VPNs da CONTRATANTE em até 10 (dez) dias úteis;  
b) Entregar relatório sucinto de transição técnica apontando o estado do backlog e tarefas pendentes;  
c) Eliminar definitivamente cópias locais de bancos de dados, dumps ou arquivos de desenvolvimento da CONTRATANTE em até 15 (quinze) dias úteis após a quitação final, ressalvada a guarda estritamente obrigatória por lei contábil ou fiscal.

---

### CLÁUSULA OITAVA – DA LIMITAÇÃO DE RESPONSABILIDADE CIVIL (DAMAGE CAP)
8.1. **Teto Máximo Indenizatório (Damage Cap):** A responsabilidade civil total e cumulativa da **CONTRATADA** perante a **CONTRATANTE** ou quaisquer terceiros por quaisquer danos diretos, perdas, custos, falhas operacionais ou prejuízos decorrentes deste Contrato, independentemente da teoria jurídica aplicada (contratual, extracontratual ou responsabilidade civil), estará **estrita e expressamente limitada à soma do valor correspondente a 3 (três) mensalidades vigentes** efetivamente pagas pela **CONTRATANTE** nos últimos 3 meses anteriores ao evento danoso.  
8.2. **Exclusão de Danos Indiretos e Lucros Cessantes:** Em nenhuma hipótese a **CONTRATADA** responderá perante a **CONTRATANTE** por: lucros cessantes, perda de receitas, perda de faturamento, paralisação de vendas, interrupção de negócios, danos indiretos, incidentais ou punitivos, nem por perda ou corrupção de dados decorrentes de falhas de serviços terceirizados, instabilidade em provedores de nuvem (AWS/GCP/Azure) ou ataques cibernéticos externos imprevisíveis.

---

### CLÁUSULA NONA – DO FORO
9.1. Para dirimir quaisquer litígios oriundos do presente instrumento, as partes elegem o **Foro da Circunscrição Judiciária de Brasília - DF**, com expressa renúncia a qualquer outro, por mais privilegiado que seja.

E, por estarem justas e contratadas, as partes firmam o presente instrumento por meio de assinatura eletrônica nos termos da Lei nº 14.063/2020.

Brasília - DF, [Dia] de [Mês] de [Ano].

_____________________________________________  
**CONTRATANTE:** [NOME DA EMPRESA CLIENTE]  
CNPJ: [00.000.000/0000-00]  

_____________________________________________  
**CONTRATADA:** TRD Consultoria e Serviços de TI LTDA  
CNPJ: [00.000.000/0001-00]  
Thiago Rocha Duarte (Sócio Administrador)  

---

## ANEXO I — PLANO DE SUSTENTAÇÃO CONTRATADO

| Plano Escolhido | Banco de Horas Mensal | SLA Resposta P1 | SLA Resposta P2 | Reuniões de Alinhamento | Valor da Hora Excedente | Investimento Fixo Mensal |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| [ ] **Plano Essencial** | 20 horas / mês | Até 8h úteis | Até 6h úteis | Mensal | R$ 200,00 / hora | **R$ 3.800,00 / mês** |
| [ ] **Plano Profissional** | 35 horas / mês | Até 4h úteis | Até 4h úteis | Quinzenal | R$ 180,00 / hora | **R$ 5.500,00 / mês** |
| [ ] **Plano Enterprise** | 60 horas / mês | Até 2h úteis | Até 2h úteis | Semanal | R$ 160,00 / hora | **R$ 8.500,00 / mês** |

*Plano selecionado formalmente pelas partes:* **[Marcar Plano Contratado]**  
*Data de Início da Vigência:* **[DD/MM/AAAA]**  
