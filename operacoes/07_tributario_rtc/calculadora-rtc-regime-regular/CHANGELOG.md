# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Mantenha um Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [1.7.2] - 2026-11-18

### Corrigido

- Arredondamento do cálculo (`POST calculadora/regime-geral` e
  `POST calculadora/observabilidade/regime-geral`): a resposta passa a fechar consigo mesma. Antes,
  as contas corriam com precisão interna de 8 casas e cada campo era arredondado sozinho na
  serialização; um total podia diferir da soma dos itens exibidos (rejeição 1091, "Total de CBS
  difere da soma dos itens") e o `vIBS` podia diferir de `vIBSUF + vIBSMun`. Agora cada número
  publicado é arredondado uma vez, no cálculo, e é ele que entra na conta seguinte. **O que muda
  para quem integra** (diferenças de centavos em relação à 1.7.0):
  - **Somas**: o `vIBS` do item é a soma do `vIBSUF` e do `vIBSMun` exibidos. Ex.: base 12,00 com
    IBS de 0,05% + 0,05%: `vIBSUF` 0.01, `vIBSMun` 0.01 e `vIBS` 0.02 (antes, 0.01).
  - **Totais**: cada total é a soma do campo exibido nos itens, e o `vIBS` total é a soma dos
    `vIBS` dos itens. Ex.: dois itens de 14,00 em 2026, `vCBS` 0.13 cada: total 0.26 (antes,
    0.25); itens de 2.315,79 e 690,52, `vCBS` 20.84 e 6.21: total 27.05 (antes, 27.06).
  - **Valores com alíquota efetiva**: a alíquota efetiva (`pAliqEfet` e a efetiva da tributação
    regular) é arredondada a 4 casas e é ela que multiplica a base. Ex.: base 1.000.000,00, CBS de
    8,8123% com redução de 60%: `pAliqEfet` 3.5249 e `vCBS` 35249.00 (antes, 35249.20, calculado
    com 3,52492%). A diferença só aparece quando a alíquota efetiva exata tem mais de 4 casas.
  - **Base**: a base (`vBC`) é arredondada a 2 casas antes de multiplicar; com Imposto Seletivo, é
    o `vIS` exibido que compõe a base do IBS e da CBS.
  - **Compra governamental**: o grupo principal traz a alíquota redistribuída com 4 casas e o
    valor calculado com ela (`vBC × pAliqEfet ÷ 100`), que não é a soma dos valores do
    `gTribCompraGov`; a diferença entre os dois grupos pode chegar a 0,04. Na transferência da CBS
    ao Estado ou ao Município comprador, a alíquota de quem cede é arredondada e a de quem recebe
    é obtida por diferença, e a soma das alíquotas exibidas é igual à soma das originais. Com
    diferimento, o `vDif` do grupo principal é calculado com a alíquota redistribuída exibida.
  - Não mudam: nomes de campos, tipos do JSON, empates (continuam indo para o par: base 25,00 a
    0,9% dá `vCBS` 0.22) e valores cuja alíquota efetiva tem até 4 casas.
- Diferimento parcial: o diferimento era descontado duas vezes do valor devido; passa a ser
  descontado uma vez (`devido = valor − vDif`). Sem efeito nas classificações atuais, que só têm
  diferimento de 100%.
- Pedágio (`POST calculadora/pedagio`): o valor de cada trecho saía com as casas da conta interna
  (ex.: 0.54545400) e a soma dos trechos não era o total. O total de cada tributo é a soma exata
  das parcelas, arredondada uma vez; cada trecho recebe a parcela truncada no centavo e os
  centavos restantes vão aos trechos de maior resto (empate: menor número de trecho). Ex.: base
  200,00 em trechos de 10, 13 e 10 km: CBS 1.80 em 0.55, 0.71 e 0.54. A alíquota efetiva do trecho
  é informativa, com 4 casas (ex.: 0.2727). Os campos continuam sendo números JSON.
- Bases unificadas (`POST calculadora/base-calculo/base-calculo-unificada` e
  `base-calculo-unificada-nfse`): as bases saem com 2 casas (ex.: 954.50, antes 954.50500000). Os
  campos continuam sendo números JSON.
- Dados abertos: percentual de redução lido do banco como `Float` é convertido pelo texto do
  número, sem passar por `double` (0.365 sairia 0.36500000953674316).
- Comparador de versões do perfil `offline` (`GET versao/status`): desde a 1.7.0 (migração para o
  Spring Boot 4.1.2, com o Jackson 3), a rota respondia 500 ("Type definition error: [...]
  VersaoOutput") em toda chamada, e a interface do pacote offline mostrava uma faixa de erro em
  cada página. A consulta à instalação online funcionava; o que falhava era a leitura da resposta,
  porque `VersaoOutput` não tem construtor que o Jackson 3 possa usar (o Jackson 2 usava o
  construtor do builder). A classe passa a ser lida pelo builder (`@Jacksonized`), a rota responde
  200 com a comparação e a subida registra a comparação no log, em vez do aviso de falha. Além
  disso:
  - **Sem acesso à instalação online** (conexão recusada, tempo esgotado, erro HTTP ou resposta
    que não é o JSON esperado), a rota também respondia 500. Passa a responder 200 com
    `versaoAplicacaoRemota`, `versaoDbRemota` e `dataDbRemota` ausentes e os três indicadores
    `false`, com um aviso no log, como a `api-simples-nacional`: o modo offline não depende da
    disponibilidade da instalação online. Na subida continuam as 3 tentativas, com um aviso ao
    final.
  - **Tempos limite**: a consulta não tinha tempo limite e podia prender a requisição numa rede
    que não responde. Passa a esperar 5 s pela conexão e 10 s pela leitura
    (`versao.connect-timeout` e `versao.read-timeout`, os mesmos nomes e valores da
    `api-simples-nacional`).
  - A leitura da resposta remota não era exercitada por nenhum teste; passa a ser, em
    `Teste_VersaoOfflineService`, sem depender de rede.

### Modificado

- `ArredondamentoUtils` passa a ser a cópia única da política numérica da Calculadora, idêntica
  byte a byte à da `api-simples-nacional`, no pacote `shared.util` (antes, `core.util`). Deixam de
  existir `PRECISAO_INTERNA`, `arredondarInterno`, `dividirPorCem`, `multiplicarPorCem` e `CEM`.
- Conta interna exata: soma, subtração, multiplicação e conversão entre percentual e fração não
  arredondam; só a divisão que pode não terminar usa 34 dígitos significativos. O avaliador de
  expressões não arredonda o resultado.
- ADR003 reescrita com a nova política numérica.

### Adicionado

- Testes de proteção: `Teste_LoteArredondamento` (lote com semente fixa e invariantes conferidas
  na resposta serializada), `Teste_CasosFixosArredondamento`,
  `Teste_CasosFixosPedagioBasesArredondamento`, `Teste_SerializacaoNaoAlteraNumero`,
  `Teste_ArquiteturaArredondamento` (nenhum arredondamento fora de `ArredondamentoUtils`) e
  `Teste_ArredondamentoUtilsCopiaUnica` (SHA-256 da classe, igual nos dois projetos).

## [1.7.1] - 2026-11-18

### Segurança

- Mitigada vulnerabilidade do OpenJDK com `-XX:-UseBASE64Intrinsics`, conforme [aviso de segurança de 17/11/2026](https://openjdk.org/groups/vulnerability/advisories/2026-11-17).

## [1.7.0] - 2026-11-16

### Adicionado

- Grupo `memoriaCalculo` (`cbs`, `ibsUF`, `ibsMun`) em `tribCalc.IBSCBS`, presente também para CSTs sem `gIBSCBS` (ex.: imunidade 410)
- Formatação pt-BR dos valores monetários e percentuais nas memórias de cálculo da CBS, do IBS e do Imposto Seletivo (ex.: `R$ 1.234,50`, `8,40%`)
- Campos `suportado` e `permiteNcmZero` em cada tipo de `GET calculadora/dados-abertos/dfe/grupos` (colunas `TPDF_B_POSSUI_TRATAMENTO_CALCULADORA` e `TPDF_B_PERMITE_NCM_ZERO` de `TIPO_DFE`)

### Modificado

- Spring Boot atualizado de `3.5.16` para `4.1.2`, para tratar vulnerabilidades identificadas pelo Dependency Track
- Java atualizado de `21` para `25` (`java.version` e `maven.compiler.release`), para tratar vulnerabilidades identificadas pelo Dependency Track
- `spring-boot-starter-web` substituído por `spring-boot-starter-webmvc`
- springdoc-openapi atualizado de `2.8.9` para `3.2.0`
- jacoco atualizado de `0.8.12` para `0.8.14`
- Lombok fixado em `1.18.48`, com `annotationProcessorPaths` no `maven-compiler-plugin`
- Spring Retry fixado em `2.0.12`
- Tracing: adicionado o starter `spring-boot-starter-opentelemetry`, e as versões de `micrometer-tracing-bridge-otel` e `opentelemetry-exporter-otlp` passam a ser gerenciadas pelo Spring Boot
- Quando o grupo `impostoSeletivo` é informado, o CST, o cClassTrib e o vínculo entre eles passam a ser validados antes da consulta de alíquota, mesmo que não haja alíquota aplicável
- Tipos de DFe NFe (tipo 55) e NFCe (tipo 65) passam a permitir NCM `00` e `00000000`  
- Tipos de DFe NFe ABI (tipo 77), DERE (tipo 94), DIR (tipo 95) e DUIMP (tipo 96), quando informados nos endpoints de cálculo (`POST calculadora/regime-geral` e `POST calculadora/observabilidade/regime-geral`), retornam erro `TipoDfeNaoTratadoException`
- Migração Flyway `V0067`: em `TIPO_DFE`, `TPDF_TIPO` passa a `NOT NULL` e são criadas as colunas `TPDF_B_PERMITE_NCM_ZERO` e `TPDF_B_POSSUI_TRATAMENTO_CALCULADORA`
- Migração Flyway `V0068`: Migração para o Simples Nacional - "Correção dos anexos do Fator R da atividade 09 (cAtivSN 09) do catálogo ATIVIDADE_SIMPLES_NACIONAL."
- Migração Flyway `V0069`: atualiza a tabela `MUNICIPIO` com as vigências segundo o site do IBGE, com as novas colunas `MUNI_INICIO_VIGENCIA` e `MUNI_FIM_VIGENCIA`
- Migração Flyway `V0070`: atualiza descrições e vigências de NCM conforme o sistema Classif e inclui o NCM `85181020`
- Migração Flyway `V0071`: Migração para o Simples Nacional - "Cálculo do Simples Nacional para todos os documentos fiscais (NF-e e NFS-e no mesmo contrato): vocabulário do regime, precisão da alíquota, máximo de itens e Imposto Seletivo por documento; regime de apuração identificado por código neutro; enquadramento dos campos de regime do documento; efeito do tpRBSN na NFS-e; indicadores de ISS no catálogo de atividades; e situações do ISS que escolhem o par de alíquotas com ou sem redistribuição do ISS."
- Migração Flyway `V0072`: atualiza a tabela `NBS` conforme o arquivo nbs2-0.csv disponibilizado na base MDIC
- Migração Flyway `V0073`: nova tabela de Classificações Tributárias (IT 2025.002 v.1.71), com nomes de `410003` e `410026`, inclusão em indDIR para `410003` e `410999`, e remoção da exceção do NCM `0710.80.00` na classificação `200014`

### Removido

- Campo `memoriaCalculo` de `gCBS`, `gIBSUF` e `gIBSMun` (substituído pelo grupo `tribCalc.IBSCBS.memoriaCalculo`)

## [1.6.2] - 2026-10-28

### Modificado

- Script de migração `V0060`: na tabela `TIPO_DFE`, criadas colunas `TPDF_B_PERMITE_NCM_ZERO` e `TPDF_B_POSSUI_TRATAMENTO_CALCULADORA` e alterada coluna `TPDF_TIPO` de `NULL` para `NOT NULL`
- Script de migração `V0061`: na tabela `ATIVIDADE_SIMPLES_NACIONAL`, corrigidos os anexos do Fator R da atividade 09 (cAtivSN 09) — `ATSN_ANEXO_SEM_FATOR_R` alterado de 'III' para 'V' e `ATSN_ANEXO_COM_FATOR_R` alterado de 'V' para 'III', conforme LC 123/2006, art. 18, §§ 5º-J e 5º-M
- Migração Flyway `V0062`: atualiza a tabela de Classificações Tributárias conforme a IT 2025.002 v.1.70, com data de atualização de 01/10/2026; inclui nove cClassTrib e dependências, ajusta classificações existentes, fundamentações legais e vínculos com tipos de DF-e
- Migração Flyway `V0063`: adiciona e carrega `CLTR_IND_GPBIODIFERENCA` e `CLTR_TPDOACAO` em `CLASSIFICACAO_TRIBUTARIA`, conforme a IT 2025.002 v.1.70, restrita às classificações do IBS/CBS
- Migração Flyway `V0064`: inclui a classificação omitida para o serviço `20.02`, NBS `106062000` e cIndOp `100301` em `CLASSIF_NBS_INDOP_LC`
- Migração Flyway `V0065`: ajusta as vigências em `CLASSIF_NBS_INDOP_LC` e `INDICADOR_OPERACAO_IBS_CBS`, passando o fim de 02/11/2026 para 30/11/2026 e o início de 03/11/2026 para 01/12/2026
- Migração Flyway `V0066`: exclui a classificação indevida `CNIL_ID = 3087` de `CLASSIF_NBS_INDOP_LC`

## [1.6.1] - 2026-10-22

### Modificado

- Atualização do ROADMAP.md

## [1.6.0] - 2026-10-13

### Modificado

- Inclusão do atributo `exigeGrupoIbsCbs` no retorno do serviço `GET /calculadora/dados-abertos/classificacoes-tributarias/cbs-ibs/{siglaDfe}/{cClassTrib}`

### Adicionado

- Endpoint de dados abertos `GET /calculadora/dados-abertos/ncm/vigentes` para listagem de NCMs vigentes (8 dígitos), com cache público de 1 hora e validação de data

## [1.5.4] - 2026-09-30

### Modificado

- Habilita a operação 820 para NFS-e, ajustando `TRTR_IN_POSSUI_AJUSTE` do registro `TRTR_ID=32` (migração Flyway `V0059`)

## [1.5.3] - 2026-09-29

### Modificado

- Ajusta as vigências de `CLASSIF_NBS_INDOP_LC` e `INDICADOR_OPERACAO_IBS_CBS`: início em 01/10/2026 passa para 03/11/2026 e fim em 30/09/2026 passa para 02/11/2026 (migração Flyway `V0058`)

## [1.5.2] - 2026-09-29

### Adicionado

- Ajuste para disponibilizar download do arquivo JAR do Simples Nacional no Portal

## [1.5.1] - 2026-09-10

### Corrigido

- Correção de typos no enum `LocalFornecimento`

## [1.5.0] - 2026-09-08

### Adicionado

- Campo `adRemIS` na resposta do Imposto Seletivo (`ImpostoSeletivoDomain`): passa a retornar a alíquota ad rem do IS, substituindo o campo depreciado `pISEspec`
- Campo `pDevTrib` na resposta de devolução de tributos (`DevolucaoTributosDomain`): passa a retornar o percentual do tributo devolvido
- Script de migração `V0048`: relações dos tipos de DF-e DIR e Duimp com as classificações tributárias da CBS, com correção das relações de NFAg e NFGas com os cClassTribs 410005 e 000002

### Modificado

- Passa a desabilitar também a NFe ABI (tipo 77) nos endpoints de cálculo, além do DERE (tipo 94), lançando `ErroFaltaImplementacaoException` quando informados
- Endpoint `POST calculadora/regime-geral`: input da operação passa a aceitar o bloco `aliquotasNominais` com `cbs`, `ibsEstadual`, `ibsMunicipal` e `impostoSeletivo` (`adValorem` e `adRem`) para permitir que o usuário informe as alíquotas a serem usadas na simulação do cálculo dos tributos
- Endpoint `POST calculadora/observabilidade/regime-geral`: input da operação passa a aceitar o bloco `aliquotasNominais` com `cbs`, `ibsEstadual`, `ibsMunicipal` e `impostoSeletivo` (`adValorem` e `adRem`) para permitir que o usuário informe as alíquotas a serem usadas na simulação do cálculo dos tributos
- Respostas de cálculo passam a expor o campo `calculoSimulado` em `ObjetoDomain`, e o teste de regressão de vários itens ignora esse campo na comparação de snapshots
- Documentação OpenAPI do header `x-warning-dados-simulados` foi detalhada (incluindo o código `6`) e replicada no endpoint de observabilidade (marcado como versão beta)
- Enumerações de observabilidade/dados simulados atualizadas com `CALCULO_SIMULADO` (`EstadoItemEnum`) e `CASO_CALCULOS_SIMULADOS` (`TipoWarningDadosSimulados`)
- No cálculo de CBS/IBS, as alíquotas nominais informadas em `aliquotasNominais` passam a ter precedência sobre as alíquotas de referência do banco (padrão, uniforme nacional, uniforme setorial, fixa e "sem alíquota"); quando a expressão de alíquota do tratamento tributário não utiliza a variável `aliquota`, o resultado da alíquota é o valor informado
- No cálculo do Imposto Seletivo, as alíquotas `adValorem` e `adRem` informadas em `aliquotasNominais.impostoSeletivo` têm precedência sobre as cadastradas; quando `adRem` é informada, a unidade de medida do grupo `impostoSeletivo` passa a ser obrigatória (erro de validação)
- Quando há alíquota de IBS (estadual ou municipal) informada, o grupo de tributação regular não é aplicado ao item

### Obsoleto

- Campo `pISEspec` na resposta do Imposto Seletivo (`ImpostoSeletivoDomain`): substituído pelo campo `adRemIS`; mantido temporariamente para compatibilidade com clientes existentes, a ser removido na próxima versão

## [1.4.0] - 2026-08-27

### Adicionado

- Endpoint de cálculo de base unificada NFS-e (`POST calculadora/base-calculo/base-calculo-unificada-nfse`): calcula as bases de cálculo IBS/CBS (regime geral) e Simples Nacional para operações de NFS-e, com suporte a documentos de ajuste (subcontratação, consórcio, intermediação, cessão de mão de obra, ressarcimento, profissional parceiro), locação de imóveis (subitem 99.03 com copropriedade proporcional), regras de vigência (ISSQN até 2032, PIS/COFINS até 2026), validações de negócio (EXXX-REGAP, EXXX-LOC-SN, E1534, LEIAUTE-XTP, E0436, BASE-NEGATIVA) e retorno unificado de avisos e erros na mesma resposta HTTP 200
- Endpoint de cálculo de base unificada (`POST calculadora/base-calculo/base-calculo-unificada`): calcula as três bases de cálculo (RG/IBS-CBS, IS, SN) para um único item, com avisos informativos para campos fora de contexto (vigência, natureza ou modelo de documento)
- Endpoint de dados abertos para consulta de atores agrupados por grupo, com filtro opcional por papel (`GET calculadora/dados-abertos/ator/grupos`)
- Endpoint de dados abertos para consulta de classificações tributárias CBS/IBS filtradas por atores da operação (fornecedor/adquirente) e por tipo de DF-e (`GET calculadora/dados-abertos/classificacoes-tributarias/cbs-ibs/por-atores`)
- Validação de tipo de documento fiscal (`tpDoc`) na operação: quando informado, valida existência, vigência, vínculo com a classificação tributária e exigência de NCM/NBS conforme nomenclatura do documento
- Validação de base de cálculo derivada: rejeita operação com base de cálculo não informada, inconsistente com o valor da operação, ou menor que a base de cálculo do Imposto Seletivo
- Serviço de validação de nomenclatura (`ValidacaoNomenclaturaService`): valida obrigatoriedade e permissão de NCM/NBS por item conforme tipo de DF-e
- Campo `tpDoc` no input de operação para indicar o tipo do documento fiscal eletrônico
- Campos `obrigatorio`, `ncmOpcional` e `nbsOpcional` no endpoint de nomenclatura aplicável por DF-e/cClassTrib
- Scripts de migração: `V0044` (grupos de DF-e), `V0045` (criação de tabelas de atores), `V0046` (carga de atores) e `V0047` (inclusão de `CLTR_TPRBSN` — Tipo de Receita Bruta do Simples Nacional — na tabela de classificações tributárias)
- Endpoints de NCM e NBS em dados abertos passam a retornar header `x-warning-dados-simulados` quando `tributadoPeloImpostoSeletivo` for true

### Modificado

- Desabilita documentos fiscais sem tratamento nos endpoints de cálculo — atualmente DERE (tipo 94), lançando `ErroFaltaImplementacaoException` quando informado
- Situações Tributárias (CST): unificação dos endpoints de CBS/IBS — `GET calculadora/dados-abertos/situacoes-tributarias/cbs-ibs/{siglaDfe}` passa a ser `GET calculadora/dados-abertos/situacoes-tributarias/cbs-ibs` com `siglaDfe` como query param opcional; quando omitido, retorna todas as situações (com classificações aninhadas e campo `id`) sem filtro de DF-e, substituindo o antigo retorno "flat" desse path
- Situações Tributárias (CST) do IS: `GET calculadora/dados-abertos/situacoes-tributarias/is/{siglaDfe}` passa a ser `GET calculadora/dados-abertos/situacoes-tributarias/is` com `siglaDfe` como query param opcional (a consulta permanece sem filtro de DF-e; com `siglaDfe` informado, mantém retorno vazio quando o IS não é aplicável ao tipo de DF-e)
- Ajustes no tratamento de erros na observabilidade
- Processamento de itens passa a receber o tipo de DF-e resolvido para aplicar validações de vínculo e nomenclatura
- Tipo de operação governamental opcional

### Corrigido

- Endpoints de consulta de NCM e NBS em dados abertos (`GET calculadora/dados-abertos/ncm` e `GET calculadora/dados-abertos/nbs`) passam a retornar 404 com problem details quando o código não é encontrado para a data informada, em vez de erro 500 (`NullPointerException`)

## [1.3.1] - 2026-08-31

### Modificado

- Script de migração `V0043`: ajusta as vigências nas tabelas `CLASSIF_NBS_INDOP_LC` e `INDICADOR_OPERACAO_IBS_CBS`; registros com início de vigência em 01/09/2026 passam a vigorar em 01/10/2026, e registros com fim de vigência em 31/08/2026 passam a vigorar até 30/09/2026

## [1.3.0] - 2026-08-13

### Modificado

- Inclusão de novos tipos de ente governamental
- Inclusão de novos tipos de operacao com ente governamental

### Corrigido

- Passando a tratar ente governamental `Distrito Federal` da mesma forma que `Estado`, para fins de compras governamentais
- Adicionado local de incidência às saídas dos endpoints `/calculadora/nfse/local-operacao` e `/calculadora/nfse/indicador-operacao` 
- Input e formatação do cTribNac passam a aceitar códigos de 4 e 6 digitos

### Adicionado

- Endpoints de validação de NBS e NCM aplicáveis (`GET /classificacoes-tributarias/nbs-aplicavel` e `GET /classificacoes-tributarias/ncm-aplicavel`)
- Enumeration `LocalIncidencia` para representar locais de incidência de tributos
- Scripts de migração para atualização das planilhas de dados dos Anexos VII e VIII da NFS-e e LC 116 (migrações `V0040`, `V0041` e `V0042`)
- Scripts `run-offline.bat` e `run-offline.ps1` para execução da aplicação no Windows

## [1.2.4] - 2026-07-09

### Adicionado

- Script de migração: `V0039` (inserir e extinguir NCMs).

## [1.2.3] - 2026-06-30

### Adicionado

- Scripts de migração: `V0035` (inclusão de novos cClassTrib), `V0036` (atualizações da tabela cClassTrib em 23/06/2026), `V0037` (inclusão de alíquotas fixas e ajustes na exceção de NCM aplicável) e `V0038` (cClassTribs 200006, 200009 e 200012 não devem mais ter ncm/nbs aplicável).

### Modificado

- `possuiPercentualReducao`: origem alterada de `TRATAMENTO_TRIBUTARIO.TRTR_IN_POSSUI_PERCENTUAL_REDUCAO` para `SITUACAO_TRIBUTARIA.SITR_IND_GRED` nos endpoints de dados abertos de classificações tributárias
- Percentuais de redução (`percentualReducaoCbs`, `percentualReducaoIbsUf`, `percentualReducaoIbsMun`) retornam `null` apenas quando não há redução na tabela de percentual de redução.

### Corrigido

- Validação de input para base de cálculo, quantidade e imposto informado para o endpoint de cálculo

## [1.2.2] - 2025-06-03

### Adicionado

- Comentários a nível de classe descrevendo todas as classes do projeto
- Decisões arquiteturais em /docs/adr
- Modelos ER do banco de dados em /docs/modelos-er

### Corrigido

- Corrige comparação de versão da aplicação no modo offline, removendo sufixo de hash de commit antes de comparar (ex.: `1.2.0-65bd4bcd` → `1.2.0`)
- Validação de número de trecho do pedágio — rejeita valores menores que 1 e números duplicados entre trechos
- Validação de município de origem e de cada trecho do pedágio — rejeita município não cadastrado ou com UF divergente
- Correção duplicata na CLASSIF_NBS_INDOP_LC para NBS 1.2504.11.00 — linha CNIL_ID=1731 restaurada para cClassTrib 000001

## [1.2.1] - 2026-05-25

### Corrigido

- problema em OffSetDateTime com offset zonado
- problema no registro das novas metricas de cache no Prometheus

## [1.2.0] - 2026-05-19

### Adicionado

- Compra Governamental: cálculo e alocação de tributos com tratamento diferenciado por ente federativo, modelo de transferência CBS, percentuais redutores para 2026 e suporte a CST de suspensão
- Compra Governamental: geração da tag de compras governamentais no XML de documentos fiscais (BPe, BPe-TM, CTe, CTe Simplificado, NFe, NF3e, NFSe) com atualização dos schemas XSD (v1.14a / v1.36 / v1.01.03 - NT004) e novas classes para modelo XML
- NFSe: endpoint para cálculo de base de cálculo, validação de NBS com fallback e testes de integração
- Indicador de Operação (cIndOp): consulta (`GET calculadora/nfse/indicador-operacao`) e validação (`POST calculadora/nfse/indicador-operacao/validate`) do indicador de operação aplicável à NFS-e a partir de NBS e data de referência
- Classificação Tributária: novos endpoints de listagem por tributo e CST (`GET calculadora/dados-abertos/classificacoes-tributarias/cbs-ibs/{cst}`, `.../imposto-seletivo/{cst}`) com data de atualização, serviço para listar cClassTrib aplicável a NBS (`GET calculadora/dados-abertos/classificacoes-tributarias/nbs`)
- NBS: serviço para listar NBS por data de vigência (`GET calculadora/dados-abertos/nbs/lista`) e NBS aplicável a cClassTrib (`GET calculadora/dados-abertos/nbs-aplicaveis`)
- Local da Operação: serviço para determinar o local da operação para fins de recolhimento do IBS (`GET calculadora/nfse/local-operacao`), com base no indicador de operação e data do fato gerador
- Dados Abertos: novos endpoints em `calculadora/dados-abertos/` para redutores de compra governamental (`/redutores-compra-governamental`), transferência CBS (`/transferencias-cbs`) e transferência IBS (`/transferencias-ibs`)
- Observabilidade: novo endpoint (`POST calculadora/observabilidade/regime-geral`) que processa todos os itens da requisição sem interromper no primeiro erro, retornando alertas e códigos de erro de forma consolidada; endpoint auxiliar para consulta descritiva dos códigos de erro (`GET calculadora/observabilidade/erros`) — funcionalidade experimental, sujeita a ajustes
- Crítica de alíquota contra unidade de medida
- Carga da tabela cClassTrib com vigência em 15/04/2026
- Inclusão do município de Boa Esperança do Norte/MT na tabela de municípios

### Modificado

- Arredondamento: revisão da lógica — cálculos intermediários mantêm maior precisão, arredondamento apenas no valor final (item ou nota); nova classe `ArredondamentoUtils` substitui `CalculadoraUtils`
- CSTs 4xx e 8xx: uso dos indicadores `gIBSCBS` da tabela de cClassTrib para determinar geração do grupo de IBS e CBS (ex.: imunidades CST 410 não geram `gIBSCBS`)
- Schemas XML: atualização de schemas para documentos fiscais (BPe/BPe-TM/CTe/CTe Simplificado/NF3e v1.14a, NFe/NFCe v1.36, NFSe v1.01.03 - NT004) com suporte a compras governamentais
- Calculadora: extração de `ProcessamentoItemService`, refatoração de `CalculoCbsIbsService` e `CalculoTributoService`
- Serializers BigDecimal: ajustes na formatação (`TDec0302_04`, `TDec1104OpRTC`, `TDec1302`)
- cClassTrib: consultas ordenadas pelo código em endpoints (`GET .../classificacoes-tributarias/cbs-ibs/{cst}`, `.../imposto-seletivo/{cst}`, `.../cbs-ibs`, `.../imposto-seletivo`) e fundamentações legais ordenadas
- cIndOp: alteração de PK, FK e CK na tabela associativa
- Exception handler: reorganização de enums/records, combinação de `CodigoErroCalculo` com `ProblemType`
- `LocalFornecimento`: atualização do enum com converter dedicado
- Memória de cálculo: ajustes em casas decimais e locale

### Obsoleto

- `GET calculadora/dados-abertos/classificacoes-tributarias/{idSituacaoTributaria}`: endpoint de listagem de cClassTrib por ID da situação tributária — substituído por `GET .../classificacoes-tributarias/cbs-ibs/{cst}` e `GET .../classificacoes-tributarias/imposto-seletivo/{cst}`, que permitem a listagem de cClassTrib por tributo e CST

### Removido

- Validação de item duplicado (`ItemDuplicadoException`): itens com mesmo número na mesma operação não são mais rejeitados, pois não representam erro tributário
- Classes obsoletas: `CalculadoraUtils`, `JsonResourceObjectMapper`, `CalculadoraServiceTest`

### Corrigido

- Retorno incorreto de alíquota de pedágio
- Alíquota padrão não retornada corretamente em endpoints de dados abertos
- Valores negativos retornados no service de cálculo de base de cálculo da NFSe
