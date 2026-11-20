# Roadmap — Calculadora de Tributos: Regime Regular

**Última atualização:** Outubro de 2026 — publicação da Release 15  
**Referência de planejamento:** Release 16 e iniciativas previstas para 2027.

Este documento apresenta as funcionalidades implementadas e as evoluções previstas para a **Calculadora de Tributos — Regime Regular**, no contexto da Reforma Tributária sobre o Consumo. O planejamento considera a evolução das regras de tributação, dos documentos fiscais eletrônicos (DF-e), dos serviços de integração e dos recursos de diagnóstico e conformidade.

## Status Atual

A Calculadora de Tributos dispõe de um **motor de cálculo determinístico**, responsável pela aplicação das regras tributárias implementadas para a CBS, o IS e o IBS. O cálculo pode ser utilizado por meio de simulador online e de componente para execução local, passível de integração aos sistemas dos contribuintes.

### Funcionalidades disponíveis

**Cálculo e enquadramento tributário**

- [x] Cálculo do IBS e da CBS conforme as regras e os tratamentos tributários implementados.
- [x] Identificação da alíquota aplicável à operação: própria, de referência ou nacionalmente uniforme.
- [x] Aplicação de percentuais de redução de alíquota previstos na legislação.
- [x] Identificação do regime tributário e da fundamentação legal relacionada ao tratamento aplicado.
- [x] Consulta e utilização das tabelas de situação tributária (CST) e classificação tributária (`cClassTrib`).
- [x] Verificação da compatibilidade de códigos NCM e NBS com os tratamentos previstos nos anexos da Lei Complementar nº 214/2025.
- [x] Aplicação de regras de arredondamento e validações dos dados de entrada.
- [x] Tratamento das operações de compras governamentais.
- [x] Tratamento do indicador e do local da operação para NFS-e.
- [x] Tratamento tributário de operações de pedágio.
- [x] Tratamento de split payment simplificado.

**Indicadores e créditos tributários**

- [x] Disponibilização de indicadores tributários associados ao código de classificação tributária.
- [x] Indicação de manutenção ou anulação do crédito da operação antecedente.
- [x] Indicação da possibilidade ou vedação de apropriação de crédito pelo adquirente.
- [x] Disponibilização de indicadores de crédito presumido para fornecedor e adquirente.

**Integração, simulação e documentos fiscais**

- [x] Simulador de cálculo online.
- [x] Componente de código aberto para execução local e integração com sistemas contábeis e de gestão (ERP).
- [x] Interfaces e documentação para integração com sistemas externos.
- [x] Consulta a dados e parâmetros tributários por serviços de dados abertos.
- [x] Geração de grupos XML preenchidos com resultados do cálculo tributário, para os DF-e abrangidos pelo Assistente de Emissão.
- [x] Verificação da conformidade dos dados de arquivos XML antes do envio para autorização, conforme os leiautes suportados.
- [x] Assistente de Emissão de DF-e, com funcionalidades de geração e validação conforme os modelos disponibilizados.
- [x] Integração com a Plataforma Nacional NFS-e e a NFS-e Via, conforme os serviços implementados.

**Rastreabilidade, diagnóstico e observabilidade**

- [x] Rastreabilidade dos parâmetros e regras empregados no cálculo.
- [x] Recursos de observabilidade na execução local, com indicação do resultado do processamento por item de documento fiscal.
- [x] Classificação de resultados de processamento, inclusive cálculo realizado, inconsistência de entrada, falha técnica e tratamento ainda não implementado.
- [x] Identificação de inconsistências relacionadas a parâmetros tributários, classificação, NCM/NBS, localização e dados da operação.
- [x] Atualização da base de dados embarcada e das regras tributárias utilizadas pela Calculadora.

### Release 15 — Outubro/2026

**Funcionalidades entregues na Release 15:**

- [x] Memória de cálculo, com detalhamento dos elementos utilizados na determinação dos tributos.
- [x] Aprimoramento do comportamento dos indicadores `ind_gIBSCBS` e `ind_gIBSCBSMono`.
- [x] Tratamento de NCM `00` ou `00000000`, conforme as regras de validação aplicáveis.
- [x] Geração de DANFSE, no escopo do aplicativo específico.
- [x] Integração com banco centralizado para o tratamento do Simples Nacional.

As funcionalidades de geração de DANFSE e de tratamento do Simples Nacional integram o ciclo de entregas da Release 15 e possuem documentação própria nos respectivos repositórios.

## Próximos Passos

### Curto Prazo — Release 16

- [ ] Implementação de funcionalidades de exportação e importação de dados ou parâmetros da operação, conforme o escopo da release.
- [ ] Evolução do tratamento das operações da Zona Franca de Manaus (ZFM).
- [ ] Aprimoramento do tratamento do tipo de operação em compras governamentais.
- [ ] Implementação das regras de cálculo do Imposto Seletivo previstas para a release.
- [ ] Tratamento do cashback na modalidade de desconto na operação, no âmbito dos documentos fiscais.
- [ ] Evolução do tratamento do Simples Nacional na NFS-e, conforme o escopo do módulo específico.
- [ ] Integração com informações do PGDAS-D (MDD).

As evoluções do Simples Nacional serão detalhadas no roadmap do módulo correspondente.

### Médio e Longo Prazo — 2027

- [ ] Evolução do tratamento da tributação monofásica de combustíveis.
- [ ] Tratamento de códigos e referências setoriais previstos na legislação, incluindo códigos ANP e referências da Anvisa, conforme a necessidade de cada operação.
- [ ] Ampliação da cobertura dos modelos e das situações tributárias de documentos fiscais eletrônicos.
- [ ] Evolução do Assistente de Emissão de DF-e e de suas rotinas de geração e validação.
- [ ] Aprimoramento das verificações de conformidade quantitativa, subjetiva e documental, com integração aos processos de tratamento de inconsistências quando aplicável.
- [ ] Evolução do tratamento de benefícios tributários dependentes da qualificação dos sujeitos da operação, conforme regulamentação e disponibilidade dos dados necessários.
- [ ] Ampliação dos tratamentos específicos de créditos presumidos e operações com bens imóveis, conforme definição de escopo.

### Iniciativas em avaliação — 2027

- [ ] Avaliação do emprego de **inteligência artificial generativa** como recurso de apoio à interpretação dos resultados da Calculadora, à explicação das regras tributárias e dos fundamentos legais aplicados e à orientação dos usuários na utilização dos serviços.

A iniciativa relacionada à inteligência artificial encontra-se **em fase de avaliação**, sem decisão de implantação. Eventual recurso de IA terá função explicativa e de apoio à interação com os usuários, **sem substituir nem modificar o caráter determinístico do motor de cálculo**. A avaliação utilizará informações públicas.

## Escopo e Limitações

A Calculadora aplica as regras tributárias implementadas e apoia a verificação de conformidade dos dados informados nos documentos fiscais. **Não substitui os sistemas de autorização de DF-e nem realiza, por si, a apuração assistida do período de apuração.** Tratamentos ainda não implementados e hipóteses dependentes de regulamentação ou de dados adicionais serão incorporados conforme o planejamento e o escopo de cada release.

## Compatibilidade com a Regulamentação

O roadmap poderá ser revisto para refletir alterações da legislação e dos atos normativos da Reforma Tributária sobre o Consumo, incluindo a **Lei Complementar nº 214/2025**, notas técnicas e tabelas de referência publicadas pelos órgãos competentes.

## Contribuições

Sugestões de funcionalidades, correções e melhorias poderão ser encaminhadas pelos canais disponibilizados no repositório, conforme a configuração e as regras de contribuição vigentes.

## Observações sobre o Planejamento

- Os itens marcados com `[x]` representam funcionalidades entregues até a Release 15, considerada a publicação conjunta deste roadmap e dessa release.
- Os itens marcados com `[ ]` representam evoluções planejadas ou iniciativas em avaliação e **não indicam conclusão**.
- A associação a uma release representa o planejamento atual e poderá ser revista em decorrência de alterações normativas, necessidades técnicas e prioridades institucionais.
- As iniciativas em avaliação não constituem compromisso de implementação ou de disponibilização em data específica.
