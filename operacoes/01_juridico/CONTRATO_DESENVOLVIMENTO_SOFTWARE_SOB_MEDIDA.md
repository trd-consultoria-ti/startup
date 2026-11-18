# INSTRUMENTO PARTICULAR DE PRESTAÇÃO DE SERVIÇOS DE DESENVOLVIMENTO DE SOFTWARE SOB MEDIDA, LICENCIAMENTO E OUTRAS AVENÇAS

Pelo presente instrumento particular, de um lado:

**CONTRATANTE:**
* **Razão Social / Nome:** [NOME DA EMPRESA CLIENTE]
* **CNPJ / CPF:** [00.000.000/0000-00]
* **Endereço:** [Endereço completo, Cidade - UF, CEP]
* **Representante Legal:** [Nome completo do representante], [Nacionalidade], [Estado Civil], portador do RG nº [0000000] e CPF nº [000.000.000-00], doravante denominada simplesmente **CONTRATANTE**;

E, de outro lado:

**CONTRATADA:**
* **Razão Social:** **THIAGO ROCHA DUARTE - CONSULTORIA E SERVICOS DE TI LTDA**
* **Nome Fantasia:** **TRD Consultoria e Serviços de TI**
* **CNPJ:** **43.065.207/0001-57** (Início de Atividade: 10/08/2021)
* **Sede:** Brasília - DF, Brasil
* **Contato Institucional:** contato@trdconsultoria.com.br | WhatsApp: +55 (61) 99952-6374
* **Representada por:** Thiago Rocha Duarte, brasileiro, engenheiro de software, na qualidade de Sócio Administrador, doravante denominada simplesmente **CONTRATADA**;

Ambas doravante denominadas conjuntamente como **PARTES** e individualmente como **PARTE**.

Têm entre si, justo e contratado, o presente Contrato, amparado na legislação brasileira aplicável, em especial o **Código Civil (Lei nº 10.406/2002)**, a **Lei do Software (Lei nº 9.609/1998)**, a **Lei Geral de Proteção de Dados (Lei nº 13.709/2018 - LGPD)**, o **Marco Civil da Internet (Lei nº 12.965/2014)** e a **Lei de Assinaturas Eletrônicas (Lei nº 14.063/2020)**, mediante as seguintes cláusulas e condições:

---

### CLÁUSULA PRIMEIRA – DO OBJETO
1.1. O presente Contrato tem por objeto a prestação de serviços especializados de engenharia de software pela **CONTRATADA** em favor da **CONTRATANTE**, visando ao planejamento, arquitetura, codificação, testes e implantação de **Sistema de Software Sob Medida** (denominado **PRODUTO**), conforme detalhado no **ANEXO I (Especificação Técnica & Escopo do MVP)**.  
1.2. O escopo da primeira versão funcional (MVP) é limitado aos módulos essenciais descritos no **ANEXO I** (ex: CRM Sob Medida, Hub de Gestão Operacional / Mini-ERP, Portal Corporativo ou Backoffice).  
1.3. **Gestão de Mudanças (Scope Creep):** Novas funcionalidades, telas, fluxos operacionais ou integrações não previstos expressamente no **ANEXO I** serão documentados e alocados prioritariamente no backlog de evolução pós-Go-Live, integrando o plano de continuidade/sustentação, ou precificados mediante Termo Aditivo específico entre as Partes.

---

### CLÁUSULA SEGUNDA – DAS ETAPAS DE EXECUÇÃO E PRAZOS
2.1. O projeto será executado pela **CONTRATADA** seguindo o cronograma ágil estabelecido no **ANEXO I**, estruturado nas seguintes fases:
* **Fase 1 — Planejamento & Arquitetura (Dia 1 ao Dia 5):** Alinhamento executivo dos fluxos de dados, prototipação de wireframes das telas principais, modelagem do banco de dados relacional (PostgreSQL) e provisionamento do ambiente na nuvem.
* **Fase 2 — Desenvolvimento & Sprints de Construção (Semanas 2 a 4):** Codificação do frontend (React / Next.js) e backend (Java / Node / Python), implementação de regras de negócio, telas operacionais e integrações.
* **Fase 3 — Homologação & Validação em Ambiente de Teste (Staging):** Disponibilização do sistema completo em ambiente de homologação para validação e testes pela **CONTRATANTE**.
* **Fase 4 — Deploy em Produção (Go-Live) & Treinamento:** Publicação definitiva da aplicação, configuração de domínio/SSL, migração de dados iniciais e sessão técnica de treinamento da equipe da **CONTRATANTE**.

2.2. **Aceite e Homologação:** A **CONTRATANTE** terá o prazo de **5 (cinco) dias úteis**, contados a partir da notificação formal de entrega pela **CONTRATADA**, para realizar os testes de homologação e apontar, em documento único consolidado, eventuais inconsistências em relação ao escopo contratado.  
2.3. Transcorrido o prazo estipulado no item 2.2 sem manifestação por escrito da **CONTRATANTE**, ou verificada a utilização operacional do sistema em ambiente de produção com clientes ou usuários reais, considerar-se-á o sistema **tácita e plenamente aceito e homologado para todos os fins de direito**, autorizando a emissão da cobrança final.  
2.4. **Custódia Técnica e Bloqueio de Deploy (Staging TRD Gate):** O desenvolvimento e os testes de homologação ocorrerão estritamente em ambiente de teste provisionado sob custódia da **CONTRATADA**. Fica expressamente estipulado que a transferência do código-fonte para o servidor final da **CONTRATANTE**, a entrega de credenciais mestre de produção e a liberação de chaves criptográficas ficam estritamente condicionadas à liquidação financeira e compensação bancária integral da Parcela Final prevista na Cláusula Terceira ou da confirmação de pagamento via cartão de crédito.

---

### CLÁUSULA TERCEIRA – DO PREÇO E CONDIÇÕES DE PAGAMENTO
3.1. Pela criação, codificação e implantação do sistema objeto deste Contrato, a **CONTRATANTE** pagará à **CONTRATADA** o valor global líquido fixado no **ANEXO I**.  
3.2. As condições de pagamento serão ajustadas conforme opção selecionada no **ANEXO I**:
* **Opção 1 — Faturado 50 / 50 (Boleto Bancário ou Pix PJ):**
  * **Entrada (50%):** Paga no ato da assinatura deste instrumento, como condição indispensável para mobilização e início dos trabalhos;
  * **Parcela Final (50%):** Paga na conclusão da homologação / Go-Live do sistema.
* **Opção 2 — À Vista via Pix / Transferência:** Pagamento integral no ato da assinatura com condição financeira bonificada acordada no Anexo I.
* **Opção 3 — Cartão de Crédito Corporativo:** Pagamento parcelado em até 12 (doze) vezes via link de transação segura processado por adquirente credenciada (Asaas / InfinitePay / Stripe / Mercado Pago), sendo os encargos operacionais de parcelamento regulados no ANEXO I.

3.3. **Emissão de Nota Fiscal:** A **CONTRATADA** emitirá as competentes Notas Fiscais de Serviços Eletrônicas (NFS-e) referentes a cada pagamento realizado, em conformidade com o enquadramento fiscal no Simples Nacional.  
3.4. **Mora e Inadimplemento:** O atraso no pagamento de qualquer parcela sujeitará a **CONTRATANTE** ao pagamento de multa moratória de 2% (dois por cento), juros de mora de 1% (um por cento) ao mês e correção monetária pelo IPCA/IBGE pro rata die, ensejando a suspensão imediata dos trabalhos ou do acesso aos ambientes de desenvolvimento após 5 (cinco) dias úteis de inadimplência.  
3.5. **Cláusula Anti-Chargeback & Renúncia a Contestação:** Caso a **CONTRATANTE** opte pelo pagamento mediante cartão de crédito (à vista ou parcelado), fica expressamente pactuado que a aprovação da homologação ou a assinatura do competente **Termo de Homologação e Entrega Definitiva** constitui ateste formal e irrevogável de recebimento do serviço prestado em sua plenitude, renunciando a **CONTRATANTE** expressamente a qualquer pedido de estorno, cancelamento unilateral ou contestação de compra (*chargeback*) perante a instituição emissora do cartão ou operadora adquirente, respondendo civilmente por perdas, danos e custas de cobrança caso venha a pleitear estorno indevido.

---

### CLÁUSULA QUARTA – DA INFRAESTRUTURA DE NUVEM E SERVIDORES (CLOUD OPS)
4.1. Os custos de servidores, bancos de dados, hospedagem em nuvem (ex: Supabase, DigitalOcean, Railway, AWS, Vercel), registro de domínios (Registro.br) e licenças de APIs de terceiros (ex: WhatsApp Business API, gateways de pagamento) são de **responsabilidade exclusiva da CONTRATANTE**.  
4.2. A contratação dos serviços de nuvem será formalizada no CNPJ e faturamento direto da **CONTRATANTE**, cabendo à **CONTRATADA** unicamente a assessoria técnica de dimensionamento, provisionamento, configuração de segurança, deploy e orquestração de infraestrutura.  
4.3. A **CONTRATADA** não assume qualquer responsabilidade por interrupções, cobranças indevidas, oscilações ou falhas decorrentes diretamente dos provedores terceiros de nuvem contratados pela **CONTRATANTE**.

---

### CLÁUSULA QUINTA – DA GARANTIA TÉCNICA PÓS-GO-LIVE
5.1. A **CONTRATADA** concede à **CONTRATANTE** o prazo de **30 (trinta) dias de Garantia Técnica Inclusa**, contados a partir da data de publicação do sistema em produção (Go-Live).  
5.2. A garantia abrange única e exclusivamente a **correção de defeitos de programação (*bugs*), desvios de regra de negócio homologada ou falhas de execução** diretamente atribuíveis ao código desenvolvido pela **CONTRATADA**, sem qualquer custo adicional para a **CONTRATANTE**.  
5.3. Estão expressamente excluídos da garantia técnica:
* Solicitações de novas funcionalidades, telas ou alterações de regras de negócio aprovadas;
* Falhas causadas por mau uso, exclusão indevida de dados por operadores da **CONTRATANTE**, alterações no código realizadas por terceiros ou indisponibilidade de serviços de terceiros/nuvem;
* Atualizações de sistemas operacionais ou navegadores supervenientes que imponham incompatibilidade de bibliotecas de terceiros.
5.4. **Interlocutor Único (SPOC) e Canal Exclusivo de Atendimento:** Para viabilizar a eficiência e a celeridade técnica, a **CONTRATANTE** designará formalmente no ANEXO I um **único interlocutor técnico/operacional (Single Point of Contact - SPOC)** com poderes para validar fluxos, homologar telas e centralizar dúvidas da equipe. Fica expressamente vedado o atendimento direto individual a colaboradores não autorizados. Todos os chamados técnicos durante o desenvolvimento e a garantia serão abertos **exclusivamente por texto e imagens/prints legíveis**, sendo sumariamente desconsideradas mensagens de áudio para fins de contagem de prazo ou abertura de ocorrência.

---

### CLÁUSULA SEXTA – DA TRANSIÇÃO PARA A SUSTENTAÇÃO CONTÍNUA (PLANO CONTINUIDADE TRD)
6.1. Ao término do período de garantia estipulado na Cláusula Quinta, a **CONTRATANTE** poderá aderir ao **Plano Continuidade TRD**, desenvolvido exclusivamente para assegurar a perenidade e evolução dos sistemas criados pela **CONTRATADA**:
* **Mensalidade:** **R$ 1.800,00 (mil e oitocentos reais) por mês**;
* **Franquia:** 10 (dez) horas técnicas mensais com SLA garantido;
* **Escopo:** Monitoramento preventivo, aplicação de patches de segurança, suporte operacional prioritário e pequenas evoluções contínuas de telas e relatórios;
* **Prazos de Atendimento (SLA):** Primeira resposta em até 4 horas úteis para incidentes P1 (sistema inoperante) e até 8 horas úteis para chamados de rotina.

6.2. Caso a **CONTRATANTE** opte por não contratar o Plano Continuidade TRD ou qualquer plano recorrente de sustentação, fica estabelecido que quaisquer intervenções técnicas futuras, suporte ou manutenções solicitadas serão faturadas por **Hora Técnica Avulsa ao valor de R$ 220,00 (duzentos e vinte reais) por hora**, executadas sem garantia de SLA e sujeitas à disponibilidade de agenda técnica da **CONTRATADA**.

---

### CLÁUSULA SÉTIMA – DA PROPRIEDADE INTELECTUAL E CÓDIGO-FONTE
7.1. Em conformidade com o Artigo 4º da **Lei nº 9.609/1998 (Lei do Software)**, a **CONTRATADA** declara que, **mediante a quitação integral do preço total avençado na Cláusula Terceira deste instrumento**, a titularidade dos direitos patrimoniais do código-fonte customizado especificamente desenvolvido para a **CONTRATANTE** será formal e integralmente transferida a esta.  
7.2. **Componentes-Base e Boilerplates TRD:** A **CONTRATADA** é e permanecerá legítima titular dos direitos autorais sobre seus frameworks proprietários, templates, bibliotecas genéricas e componentes reutilizáveis (*TRD Core Boilerplate*) preexistentes incorporados ao sistema. A **CONTRATADA** concede à **CONTRATANTE** uma **licença perpétua, irrevogável, mundial, não exclusiva e livre de royalties adicionais** para uso, execução, compilação e modificação de tais componentes internamente no sistema contratado, sendo vedada a comercialização, sublicenciamento ou distribuição autônoma dos templates base da **CONTRATADA** a terceiros.  
7.3. **Condição Resolutiva de Retenção:** Caso ocorra inadimplemento financeiro por parte da **CONTRATANTE**, a transferência dos direitos patrimoniais de propriedade intelectual e a entrega definitiva do repositório/código-fonte mestre permanecerão expressamente suspensas até a liquidação integral das obrigações financeiras.

---

### CLÁUSULA OITAVA – DA LIMITAÇÃO DE RESPONSABILIDADE CIVIL (*DAMAGE CAP*)
8.1. A responsabilidade civil total acumulada da **CONTRATADA** perante a **CONTRATANTE**, sob qualquer fundamento jurídico (contratual, extracontratual ou civil), por perdas, danos, falhas de sistema ou penalidades decorrentes da prestação dos serviços deste Contrato, fica estritamente **limitada ao teto máximo (*Damage Cap*) correspondente a 100% (cem por cento) do valor total efetivamente pago pela CONTRATANTE à CONTRATADA** no âmbito deste instrumento.  
8.2. Em nenhuma hipótese qualquer das Partes responderá por **danos indiretos, lucros cessantes, perda de receita, perda de oportunidade de negócios, custos de interrupção operacional ou perda de reputação comercial**, ainda que alertada da possibilidade de tais ocorrências.

---

### CLÁUSULA NONA – DA CONFIDENCIALIDADE E PROTEÇÃO DE DADOS (LGPD)
9.1. As Partes comprometem-se a manter sob o mais absoluto sigilo todas as informações comerciais, operacionais, técnicas, segredos de negócio e dados cadastrais a que tiverem acesso em virtude deste Contrato, pelo prazo de **5 (cinco) anos** após o seu término.  
9.2. No tratamento de dados pessoais no âmbito da prestação dos serviços, as Partes atuarão em estrita conformidade com a **Lei Federal nº 13.709/2018 (LGPD)**, figurando a **CONTRATANTE** como **Controladora** e a **CONTRATADA** estritamente como **Operadora** técnica, implementando medidas técnicas e administrativas compatíveis para segurança da informação.

---

### CLÁUSULA DÉCIMA – DA VALIDADE JURÍDICA DAS ASSINATURAS ELETRÔNICAS E FORO
10.1. As Partes reconhecem a plena validade jurídica, integridade e eficácia executiva do presente instrumento assinado eletronicamente por meio de certificados digitais ICP-Brasil ou plataformas de assinatura eletrônica avançada (ex: ZapSign, ClickSign ou Gov.br), nos termos do Art. 10 da Medida Provisória nº 2.200-2/2001 e da **Lei Federal nº 14.063/2020**.  
10.2. Para dirimir quaisquer litígios oriundos deste Contrato que não possam ser solucionados consensualmente entre as Partes, fica eleito o **Foro da Circunscrição Judiciária Especial de Brasília - Distrito Federal**, com renúncia expressa a qualquer outro, por mais privilegiado que seja.  
10.3. **Eficácia de Título Executivo Extrajudicial:** As Partes atribuem ao presente instrumento a força e eficácia de título executivo extrajudicial, nos exatos termos do **Art. 784, inciso III, do Código de Processo Civil (Lei Federal nº 13.105/2015)**.

E, por estarem assim justas e acordadas, as Partes assinam o presente Contrato eletronicamente na presença de 2 (duas) testemunhas.

Brasília - DF, [DD] de [Mês] de [202X].

---

**Pela CONTRATANTE:**  
____________________________________________________  
**[NOME DO REPRESENTANTE LEGAL]**  
[Cargo - Ex: Sócio Administrador / Diretor]  
[Nome da Empresa Contratante]

---

**Pela CONTRATADA:**  
____________________________________________________  
**THIAGO ROCHA DUARTE**  
Sócio Administrador & Arquiteto Chefe  
**THIAGO ROCHA DUARTE - CONSULTORIA E SERVICOS DE TI LTDA**  
CNPJ: 43.065.207/0001-57

---

**Testemunhas:**

1. _________________________________  
Nome:  
CPF:  

2. _________________________________  
Nome:  
CPF:  

---

# ANEXO I — ESPECIFICAÇÃO DO PROJETO, ESCOPO E INVESTIMENTO

| Metadado do Projeto | Especificação |
| :--- | :--- |
| **Nome do Sistema / Projeto** | [Ex: Sistema CRM Sob Medida / Hub Operacional TRD] |
| **Modalidade Contratada** | [ ] Starter (R$ 9.800) &nbsp;&nbsp;&nbsp;&nbsp; [ ] Pro (R$ 16.500 a R$ 18.000) &nbsp;&nbsp;&nbsp;&nbsp; [ ] Sob Medida Específico |
| **Prazo Estimado de Entrega** | [Ex: 3 a 4 semanas úteis] |
| **Garantia Técnica Pós-Go-Live** | 30 (trinta) dias corridos inclusos |
| **Plano Pós-Garantia Opcional** | [x] Plano Continuidade TRD (R$ 1.800/mês para 10h mensais com SLA) |

### 1. Escopo Delimitado da Versão MVP (Até 3 Módulos Core):
* **Módulo 1 — Controle de Acesso & Cadastros Base:** [Descrever: Login, RBAC Administrador/Operador, cadastro de clientes, fornecedores ou produtos].
* **Módulo 2 — Motor Operacional Principal:** [Descrever: Funil de Vendas / Emissão de Orçamentos / Ordens de Serviço / Integração WhatsApp].
* **Módulo 3 — Relatórios, Fechamento & Exportação:** [Descrever: Painel financeiro básico, conciliação, geração de relatórios PDF/Excel].

### 2. Stack Tecnológica Acordada:
* **Frontend:** React / Next.js (TypeScript) + Tailwind CSS;
* **Backend:** [Java 21 / Spring Boot 3 &bull; Node.js / TypeScript &bull; Python / FastAPI];
* **Banco de Dados:** PostgreSQL;
* **Hospedagem em Nuvem:** [Provedor no CNPJ do cliente — ex: Supabase / DigitalOcean / Railway / AWS].

### 3. Condição Financeira e Forma de Pagamento Selecionada:
* **Valor Global do Projeto:** **R$ [Valor em Reais]**;
* **Condição de Pagamento:**
  * [ ] **À Vista no Pix:** R$ [Valor] quitado na assinatura deste contrato;
  * [ ] **Faturado 50 / 50:** Entrada de R$ [Valor] na assinatura + Parcela final de R$ [Valor] na homologação;
  * [ ] **Cartão de Crédito:** R$ [Valor] parcelado em [Número]x de R$ [Valor] via link seguro de pagamento.