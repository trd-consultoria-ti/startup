-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-10-20
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-10-20'),
    'V0071',
    'Cálculo do Simples Nacional para todos os documentos fiscais (NF-e e NFS-e no mesmo contrato): vocabulário do regime, precisão da alíquota, máximo de itens e Imposto Seletivo por documento; regime de apuração identificado por código neutro; enquadramento dos campos de regime do documento; efeito do tpRBSN na NFS-e; indicadores de ISS no catálogo de atividades; e situações do ISS que escolhem o par de alíquotas com ou sem redistribuição do ISS.'
);

-- -----------------------------------------------------------------------------------------------
-- Descrição (documento 07 da api-simples-nacional, "Cálculo do Simples Nacional para todos os
-- documentos fiscais — requisitos", seção 11.1):
-- 1. TIPO_DFE_CALCULO_SIMPLES_NACIONAL: novas colunas de vocabulário do regime, casas decimais
--    da alíquota no cálculo, máximo de itens e admissão do Imposto Seletivo
-- 2. REGIME_APURACAO_SIMPLES_NACIONAL: reconstrução da tabela (código neutro do regime e
--    indicador "calcula tributos"; saem as colunas de CRT e tpRegimeIBSCBS), preservando IDs,
--    vigências e comportamento das 3 linhas atuais; linhas novas PENDENTE_SUBLIMITE e
--    SOMENTE_RECEITA
-- 3. Criação e carga da tabela ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL (combinação dos campos de
--    regime do documento ⇒ regime de apuração)
-- 4. TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL: nova coluna de vocabulário e linhas da NFS-e
-- 5. ATIVIDADE_SIMPLES_NACIONAL: novas colunas "ISS no Simples" e "admite benefício municipal"
-- 6. Criação e carga da tabela SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL (fatos do documento
--    que levam ao par de alíquotas sem redistribuição do ISS)
--
-- Todas as tabelas são EXCLUSIVAS do cálculo do Simples Nacional. Em código ficam só a
-- validação estrutural dos leiautes (domínios, campos proibidos por família, coerência das
-- regras C21a-10/20/30 da NT 2026.005 e 291, 292 e 313 da NFS-e), a ordem de avaliação das
-- regras do par de alíquotas e o cálculo propriamente dito.
--
-- O endpoint clássico (POST calculadora/observabilidade/simples-nacional) continua produzindo
-- exatamente os resultados de antes desta migração: ele usa só as linhas de vocabulário CRT e
-- as linhas padrão (vocabulário nulo) do efeito do tpRBSN.
--
-- Ordem de implantação: a api-simples-nacional que lê estas colunas só pode ir para um
-- ambiente depois que o banco com esta migração estiver publicado nele.
-- -----------------------------------------------------------------------------------------------

-- ==========================================================================================
-- 1. TIPO_DFE_CALCULO_SIMPLES_NACIONAL: características do cálculo por documento
-- ==========================================================================================
--
--   TDCS_VOCABULARIO_REGIME: em que campos o documento informa o regime do emitente:
--     'CRT'  = CRT e tpRegimeIBSCBS (NF-e, NFC-e, CT-e, CT-e OS, BP-e, BP-e TM e NFCom);
--     'NFSE' = opSimpNac, regApIBSCBSSN e regApTribSN (DPS da NFS-e, grupo prest/regTrib,
--              AnexoVI v1.04.01, campos 151 a 153);
--   TDCS_CASAS_ALIQUOTA_CALCULO: casas decimais da alíquota USADA no cálculo. Nula = precisão
--     integral (ADR001). Na NFS-e, 2 casas: os campos pIBSSN e pCBSSN do gTribSN são 1-2V2
--     (AnexoVI, campos 102 e 104) e o valor sai de base × alíquota de 2 casas (regras 200, 202,
--     205 e 206). O arredondamento é feito DEPOIS da transformação da compra governamental;
--   TDCS_MAXIMO_ITENS: número máximo de itens da operação. Nulo = sem limite. Na NFS-e, 1 (a DPS
--     tem um só serviço: serv 0-1, um cTribNac, um cClassTrib);
--   TDCS_IN_ADMITE_IS: 1 = o documento admite Imposto Seletivo (valor repassado); 0 = não
--     admite (a NFS-e não tem IS: valor diferente de zero é erro VSN-035).
--
-- Os DEFAULT existem para viabilizar o ALTER no SQLite; a carga abaixo é explícita para todas
-- as linhas (padrão destes scripts).
-- Referências de TPDF_ID: 2 = NF-e (55), 3 = NFC-e (65), 4 = CT-e (57), 5 = CT-e OS (67),
-- 6 = BP-e (63), 8 = BP-e TM (93), 10 = NFS-e (91), 12 = NFCom (62).

ALTER TABLE TIPO_DFE_CALCULO_SIMPLES_NACIONAL ADD COLUMN TDCS_VOCABULARIO_REGIME TEXT NOT NULL DEFAULT 'CRT' CHECK (TDCS_VOCABULARIO_REGIME IN ('CRT', 'NFSE'));
ALTER TABLE TIPO_DFE_CALCULO_SIMPLES_NACIONAL ADD COLUMN TDCS_CASAS_ALIQUOTA_CALCULO INTEGER DEFAULT NULL CHECK (TDCS_CASAS_ALIQUOTA_CALCULO IS NULL OR (TDCS_CASAS_ALIQUOTA_CALCULO >= 0 AND TDCS_CASAS_ALIQUOTA_CALCULO <= 34));
ALTER TABLE TIPO_DFE_CALCULO_SIMPLES_NACIONAL ADD COLUMN TDCS_MAXIMO_ITENS INTEGER DEFAULT NULL CHECK (TDCS_MAXIMO_ITENS IS NULL OR TDCS_MAXIMO_ITENS >= 1);
ALTER TABLE TIPO_DFE_CALCULO_SIMPLES_NACIONAL ADD COLUMN TDCS_IN_ADMITE_IS INTEGER NOT NULL DEFAULT 1 CHECK (TDCS_IN_ADMITE_IS IN (0, 1));

-- Documentos com CRT: precisão integral, sem limite de itens, admitem IS.
UPDATE TIPO_DFE_CALCULO_SIMPLES_NACIONAL
   SET TDCS_VOCABULARIO_REGIME = 'CRT', TDCS_CASAS_ALIQUOTA_CALCULO = NULL, TDCS_MAXIMO_ITENS = NULL, TDCS_IN_ADMITE_IS = 1
 WHERE TDCS_TPDF_ID IN (2, 3, 4, 5, 6, 8, 12);

-- NFS-e: vocabulário próprio, alíquota de 2 casas no cálculo, 1 item, sem IS.
UPDATE TIPO_DFE_CALCULO_SIMPLES_NACIONAL
   SET TDCS_VOCABULARIO_REGIME = 'NFSE', TDCS_CASAS_ALIQUOTA_CALCULO = 2, TDCS_MAXIMO_ITENS = 1, TDCS_IN_ADMITE_IS = 0
 WHERE TDCS_TPDF_ID = 10;

-- ==========================================================================================
-- 2. REGIME_APURACAO_SIMPLES_NACIONAL: código neutro do regime (reconstrução da tabela)
-- ==========================================================================================
--
-- A tabela passa a ser identificada por um CÓDIGO NEUTRO, que não depende do documento:
--
--   SN_PLENO           = IBS e CBS pelo Simples;
--   SN_SUBLIMITE       = só a CBS pelo Simples (excesso de sublimite);
--   PENDENTE           = opção pelo Simples pendente: tudo fica pendente/suspenso;
--   PENDENTE_SUBLIMITE = opção pendente, só a CBS (HIPÓTESE — pendência P1 do documento 07:
--                        a semântica é trocável por script nesta linha);
--   SOMENTE_RECEITA    = optou pelo regime regular do IBS e da CBS: o evento leva só a receita
--                        bruta (vRBSNitem, tpRBSN e os totais) e os tributos não são
--                        calculados.
--
--   RASN_IN_CALCULA_TRIBUTOS: 0 = os tributos não são calculados (SOMENTE_RECEITA): o item sai
--     NAO_CALCULADO, só com a receita bruta. Nesse caso as colunas de IBS e destinos não se
--     aplicam e ficam nulas (CHECK abaixo);
--   RASN_IN_GERA_IBS, RASN_DESTINO_DEVIDO, RASN_DESTINO_PENDENTE_SUSPENSO: como antes.
--
-- As colunas RASN_CRT e RASN_TPREGIMEIBSCBS saem: a combinação dos campos do documento vai
-- para a tabela de enquadramento (seção 3), que também cobre o vocabulário da NFS-e. O SQLite
-- não altera restrições nem remove colunas com CHECK de forma segura; a tabela é reconstruída
-- preservando IDs, descrições, vigências e comportamento das 3 linhas atuais. Nenhuma outra
-- tabela referencia esta até aqui.

ALTER TABLE REGIME_APURACAO_SIMPLES_NACIONAL RENAME TO REGIME_APURACAO_SIMPLES_NACIONAL_OLD;

CREATE TABLE REGIME_APURACAO_SIMPLES_NACIONAL (
    RASN_ID INTEGER NOT NULL,
    RASN_CD TEXT NOT NULL CHECK (RASN_CD IN ('SN_PLENO', 'SN_SUBLIMITE', 'PENDENTE', 'PENDENTE_SUBLIMITE', 'SOMENTE_RECEITA')),
    RASN_DESCRICAO TEXT NOT NULL,
    RASN_IN_CALCULA_TRIBUTOS INTEGER NOT NULL CHECK (RASN_IN_CALCULA_TRIBUTOS IN (0, 1)),
    RASN_IN_GERA_IBS INTEGER DEFAULT NULL CHECK (RASN_IN_GERA_IBS IS NULL OR RASN_IN_GERA_IBS IN (0, 1)),
    RASN_DESTINO_DEVIDO TEXT DEFAULT NULL CHECK (RASN_DESTINO_DEVIDO IS NULL OR RASN_DESTINO_DEVIDO IN ('DEVIDO', 'PENDENTE_SUSPENSO')),
    RASN_DESTINO_PENDENTE_SUSPENSO TEXT DEFAULT NULL CHECK (RASN_DESTINO_PENDENTE_SUSPENSO IS NULL OR RASN_DESTINO_PENDENTE_SUSPENSO IN ('PENDENTE_SUSPENSO', 'ZERO')),
    RASN_INICIO_VIGENCIA TEXT NOT NULL,
    RASN_FIM_VIGENCIA TEXT DEFAULT NULL CHECK ("RASN_FIM_VIGENCIA" IS NULL OR "RASN_FIM_VIGENCIA" >= "RASN_INICIO_VIGENCIA"),
    PRIMARY KEY (RASN_ID),
    -- Regime que calcula tem de dizer se gera IBS e para onde vão os valores.
    CHECK (RASN_IN_CALCULA_TRIBUTOS = 0
        OR (RASN_IN_GERA_IBS IS NOT NULL AND RASN_DESTINO_DEVIDO IS NOT NULL AND RASN_DESTINO_PENDENTE_SUSPENSO IS NOT NULL))
);

-- As 3 linhas atuais, com os mesmos IDs e o código neutro correspondente à combinação antiga:
-- (CRT 1, sem tpRegime) ⇒ SN_PLENO; (CRT 2, sem tpRegime) ⇒ SN_SUBLIMITE; (CRT 3, tpRegime 2) ⇒ PENDENTE.
INSERT INTO REGIME_APURACAO_SIMPLES_NACIONAL (
    RASN_ID, RASN_CD, RASN_DESCRICAO, RASN_IN_CALCULA_TRIBUTOS, RASN_IN_GERA_IBS,
    RASN_DESTINO_DEVIDO, RASN_DESTINO_PENDENTE_SUSPENSO, RASN_INICIO_VIGENCIA, RASN_FIM_VIGENCIA
)
SELECT
    RASN_ID,
    CASE
        WHEN RASN_CRT = 1 AND RASN_TPREGIMEIBSCBS IS NULL THEN 'SN_PLENO'
        WHEN RASN_CRT = 2 AND RASN_TPREGIMEIBSCBS IS NULL THEN 'SN_SUBLIMITE'
        WHEN RASN_CRT = 3 AND RASN_TPREGIMEIBSCBS = 2 THEN 'PENDENTE'
    END,
    RASN_DESCRICAO,
    1,
    RASN_IN_GERA_IBS,
    RASN_DESTINO_DEVIDO,
    RASN_DESTINO_PENDENTE_SUSPENSO,
    RASN_INICIO_VIGENCIA,
    RASN_FIM_VIGENCIA
FROM REGIME_APURACAO_SIMPLES_NACIONAL_OLD;

DROP TABLE REGIME_APURACAO_SIMPLES_NACIONAL_OLD;

-- PENDENTE_SUBLIMITE (NFS-e: opSimpNac 4 + regApIBSCBSSN 2): só a CBS, toda pendente/suspensa.
-- HIPÓTESE (pendência P1): o AnexoVI prevê o gTribSN só com pCBSSN e vCBSSN (regra 198), mas
-- não diz o destino. Adotado o mesmo destino do PENDENTE, sem o IBS.
INSERT INTO REGIME_APURACAO_SIMPLES_NACIONAL VALUES
(4, 'PENDENTE_SUBLIMITE', 'Opção pelo Simples Nacional pendente, só a CBS pelo Simples (hipótese: CBS integralmente pendente/suspensa; IBS fora do evento)', 1, 0, 'PENDENTE_SUSPENSO', 'ZERO', '2027-01-01', NULL);

-- SOMENTE_RECEITA: optante pelo regime regular do IBS e da CBS. O evento leva só a receita bruta
-- (NT 2026.005, seções 5.3.1 e 5.3.4, observação (*); na NFS-e, regApIBSCBSSN 3: sem gTribSN,
-- com vReceitaBrutaSN).
INSERT INTO REGIME_APURACAO_SIMPLES_NACIONAL VALUES
(5, 'SOMENTE_RECEITA', 'Regime regular do IBS e da CBS: só a receita bruta, sem cálculo de tributos', 0, NULL, NULL, NULL, '2027-01-01', NULL);

-- ==========================================================================================
-- 3. Criação e carga da tabela ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL
-- ==========================================================================================
--
-- Combinação dos campos de regime do documento ⇒ regime de apuração (código neutro). Cada
-- linha vale para um vocabulário:
--
--   'CRT'  : ERSN_CRT obrigatório; ERSN_TPREGIMEIBSCBS nulo = documento sem a tag;
--            ERSN_OPSIMPNAC e ERSN_REGAPIBSCBSSN nulos;
--   'NFSE' : ERSN_OPSIMPNAC e ERSN_REGAPIBSCBSSN obrigatórios; ERSN_CRT e ERSN_TPREGIMEIBSCBS
--            nulos. O regApTribSN NÃO entra aqui: ele não diz nada sobre IBS e CBS (o leiaute
--            diz isso expressamente) e só participa da escolha do par de alíquotas (seção 6).
--
-- A validação estrutural (domínios, campos proibidos por família e as coerências C21a-10/20/30,
-- 291, 292 e 313) fica em código e acontece ANTES desta consulta. Combinação válida no leiaute
-- mas SEM linha vigente aqui está fora do escopo do cálculo (HTTP 422, VSN-019): CRT 3 sem
-- tpRegimeIBSCBS (regime normal), CRT 4 (MEI), opSimpNac 1 (não optante) e 2 (MEI).
--
-- O endpoint clássico usa só as linhas 'CRT' 1, 2 e 3+2, com os mesmos resultados de antes. As
-- linhas 1+1 e 2+1 (SOMENTE_RECEITA) só valem nos endpoints novos: o clássico continua recusando
-- tpRegimeIBSCBS = 1 em código (VSN-019), antes de consultar esta tabela.

CREATE TABLE ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL (
    ERSN_ID INTEGER NOT NULL,
    ERSN_VOCABULARIO_REGIME TEXT NOT NULL CHECK (ERSN_VOCABULARIO_REGIME IN ('CRT', 'NFSE')),
    ERSN_CRT INTEGER DEFAULT NULL CHECK (ERSN_CRT IS NULL OR ERSN_CRT IN (1, 2, 3, 4)),
    ERSN_TPREGIMEIBSCBS INTEGER DEFAULT NULL CHECK (ERSN_TPREGIMEIBSCBS IS NULL OR ERSN_TPREGIMEIBSCBS IN (1, 2)),
    ERSN_OPSIMPNAC INTEGER DEFAULT NULL CHECK (ERSN_OPSIMPNAC IS NULL OR ERSN_OPSIMPNAC IN (1, 2, 3, 4)),
    ERSN_REGAPIBSCBSSN INTEGER DEFAULT NULL CHECK (ERSN_REGAPIBSCBSSN IS NULL OR ERSN_REGAPIBSCBSSN IN (1, 2, 3)),
    ERSN_RASN_ID INTEGER NOT NULL,
    ERSN_INICIO_VIGENCIA TEXT NOT NULL,
    ERSN_FIM_VIGENCIA TEXT DEFAULT NULL CHECK ("ERSN_FIM_VIGENCIA" IS NULL OR "ERSN_FIM_VIGENCIA" >= "ERSN_INICIO_VIGENCIA"),
    PRIMARY KEY (ERSN_ID),
    FOREIGN KEY (ERSN_RASN_ID) REFERENCES REGIME_APURACAO_SIMPLES_NACIONAL(RASN_ID),
    -- Os campos preenchidos têm de ser os do vocabulário da linha.
    CHECK ((ERSN_VOCABULARIO_REGIME = 'CRT' AND ERSN_CRT IS NOT NULL AND ERSN_OPSIMPNAC IS NULL AND ERSN_REGAPIBSCBSSN IS NULL)
        OR (ERSN_VOCABULARIO_REGIME = 'NFSE' AND ERSN_CRT IS NULL AND ERSN_TPREGIMEIBSCBS IS NULL
            AND ERSN_OPSIMPNAC IS NOT NULL AND ERSN_REGAPIBSCBSSN IS NOT NULL))
);

-- Vocabulário CRT (documento 07, seção 3.2). Referências de RASN_ID: 1 = SN_PLENO,
-- 2 = SN_SUBLIMITE, 3 = PENDENTE, 4 = PENDENTE_SUBLIMITE, 5 = SOMENTE_RECEITA.
--                                                         (id, vocab, CRT, tpRegime, opSimpNac, regApIBSCBSSN, regime, vigências)
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (1,  'CRT',  1,    NULL, NULL, NULL, 1, '2027-01-01', NULL); -- CRT 1                 ⇒ SN_PLENO
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (2,  'CRT',  2,    NULL, NULL, NULL, 2, '2027-01-01', NULL); -- CRT 2                 ⇒ SN_SUBLIMITE
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (3,  'CRT',  1,    1,    NULL, NULL, 5, '2027-01-01', NULL); -- CRT 1 + tpRegime 1     ⇒ SOMENTE_RECEITA
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (4,  'CRT',  2,    1,    NULL, NULL, 5, '2027-01-01', NULL); -- CRT 2 + tpRegime 1     ⇒ SOMENTE_RECEITA
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (5,  'CRT',  3,    2,    NULL, NULL, 3, '2027-01-01', NULL); -- CRT 3 + tpRegime 2     ⇒ PENDENTE

-- Vocabulário NFSE (documento 07, seção 3.3).
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (6,  'NFSE', NULL, NULL, 3,    1,    1, '2027-01-01', NULL); -- ME/EPP + IBS e CBS pelo SN     ⇒ SN_PLENO
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (7,  'NFSE', NULL, NULL, 3,    2,    2, '2027-01-01', NULL); -- ME/EPP + só a CBS pelo SN      ⇒ SN_SUBLIMITE
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (8,  'NFSE', NULL, NULL, 3,    3,    5, '2027-01-01', NULL); -- ME/EPP + regime regular        ⇒ SOMENTE_RECEITA
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (9,  'NFSE', NULL, NULL, 4,    1,    3, '2027-01-01', NULL); -- pendente + IBS e CBS pelo SN   ⇒ PENDENTE
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (10, 'NFSE', NULL, NULL, 4,    2,    4, '2027-01-01', NULL); -- pendente + só a CBS pelo SN    ⇒ PENDENTE_SUBLIMITE
INSERT INTO ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL VALUES (11, 'NFSE', NULL, NULL, 4,    3,    5, '2027-01-01', NULL); -- pendente + regime regular      ⇒ SOMENTE_RECEITA

-- ==========================================================================================
-- 4. TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL: efeito do tpRBSN por vocabulário
-- ==========================================================================================
--
--   TRBS_VOCABULARIO_REGIME: nulo = linha PADRÃO (a de sempre, usada pela família CRT e pelo
--     endpoint clássico); 'NFSE' = linha específica da NFS-e. A busca procura primeiro a linha
--     do vocabulário do documento e, se não houver, a linha padrão.
--
-- Linhas da NFS-e (AnexoVI v1.04.01): o valor do gTribSN sai só com tpRBSN 1 (regras 203/204 e
-- 207/208); com qualquer outro código os valores são zero, sem pendente/suspenso. A receita bruta
-- (vRBSNitem) segue a regra padrão (tpRBSN 0 ⇒ 0; senão, vReceitaBrutaSN — regras 105 e 106). A
-- alíquota só é exigida no código 1. O indicador de vedação para não tributável (regra UB14-90) e
-- a descrição são os da linha padrão.

ALTER TABLE TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL ADD COLUMN TRBS_VOCABULARIO_REGIME TEXT DEFAULT NULL CHECK (TRBS_VOCABULARIO_REGIME IS NULL OR TRBS_VOCABULARIO_REGIME IN ('CRT', 'NFSE'));

--                                                             (id, cd, descricao, compoeReceita, isCompoeReceita, fatorReceita, pendenteSuspenso, exigeAliquota, vedadoNaoTributavel, vigencias, vocabulario)
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (8,  0, 'Não é receita bruta',                               0, 0, 0.0, 0, 0, 0, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (9,  1, 'Receita bruta - interna',                           1, 0, 1.0, 0, 1, 1, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (10, 2, 'Receita bruta - interna sem cálculo de IBS e CBS',  1, 0, 0.0, 0, 0, 1, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (11, 3, 'Receita bruta - exportação direta',                 1, 0, 0.0, 0, 0, 1, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (12, 4, 'Receita bruta - exportação indireta',               1, 0, 0.0, 0, 0, 1, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (13, 5, 'Receita bruta - mercado interno e exportação',      1, 0, 0.0, 0, 0, 1, '2027-01-01', NULL, 'NFSE');
INSERT INTO TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL VALUES (14, 9, 'Fornecimento incompatível com SN',                  1, 0, 0.0, 0, 0, 0, '2027-01-01', NULL, 'NFSE');

-- ==========================================================================================
-- 5. ATIVIDADE_SIMPLES_NACIONAL: indicadores de ISS
-- ==========================================================================================
--
--   ATSN_IN_ISS_NO_SIMPLES: 1 = o percentual do ISS entra no cálculo do Simples da atividade.
--     Sem ISS no Simples (0) o item usa o par de alíquotas SEM redistribuição do ISS (regra 5 do
--     par): Res. CGSN 140, art. 25, § 1º, VI (locação, "deduzida a parcela correspondente ao
--     ISS"), VIII (escritório contábil com ISS fixo), IX (transporte intermunicipal e
--     interestadual e comunicação, "desconsiderando-se o percentual relativo ao ISS") e X
--     (serviços sem ISS). Também decide se tribISSQN e tpRetISSQN são exigidos no item;
--   ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL: 1 = a atividade admite benefício municipal do ISS
--     (tpBM). Regra E0536 do Anexo I da NFS-e: o optante que apura o ISS pelo Simples só tem
--     benefício municipal nos subitens 7.02 e 7.05 (atividades 12 e 13). Nas demais, o tpBM
--     informado é desconsiderado, com aviso.
--
-- As atividades 01 a 04 (anexos I e II) e o código 90 ficam com 0: não têm ISS na repartição e
-- se resolvem antes, pelas regras 2 e 3 do par. A vigência do catálogo (2027-01-01) ainda não
-- começou; as colunas novas são carregadas por UPDATE, como na V0061.

ALTER TABLE ATIVIDADE_SIMPLES_NACIONAL ADD COLUMN ATSN_IN_ISS_NO_SIMPLES INTEGER NOT NULL DEFAULT 0 CHECK (ATSN_IN_ISS_NO_SIMPLES IN (0, 1));
ALTER TABLE ATIVIDADE_SIMPLES_NACIONAL ADD COLUMN ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL INTEGER NOT NULL DEFAULT 0 CHECK (ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL IN (0, 1));

UPDATE ATIVIDADE_SIMPLES_NACIONAL SET ATSN_IN_ISS_NO_SIMPLES = 0, ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL = 0;
UPDATE ATIVIDADE_SIMPLES_NACIONAL SET ATSN_IN_ISS_NO_SIMPLES = 1 WHERE ATSN_CATIVSN IN ('07', '09', '10', '12', '13', '14');
UPDATE ATIVIDADE_SIMPLES_NACIONAL SET ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL = 1 WHERE ATSN_CATIVSN IN ('12', '13');

-- ==========================================================================================
-- 6. Criação e carga da tabela SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL
-- ==========================================================================================
--
-- Fatos do documento que, para o item, tiram o percentual do ISS do cálculo do Simples e por
-- isso levam ao par de alíquotas SEM redistribuição do ISS (documento 07, seção 6). Princípio
-- (Solução de Consulta Cosit 298/2019; Fale Conosco da RFB, 2018 e 2021): a redistribuição só
-- existe para levar aos outros tributos o ISS que passa do teto; sem ISS no cálculo não há
-- excedente.
--
--   SIPA_CAMPO / SIPA_VALOR: campo do documento (nome do contrato da API) e valor;
--   SIPA_REGRA: a regra do par (regraAliquota na saída da API):
--     ISS_FORA_DO_SIMPLES (regra 4) - CRT 2 e regApIBSCBSSN 2 (excesso de sublimite: Res. CGSN
--       140, art. 12, na redação de 2027) e regApTribSN 2 e 3 (ISS fora do Simples). O
--       impedimento pelo sublimite vem da linha de alíquotas, não do documento, e por isso é
--       avaliado em código junto com estas linhas;
--     ISS_NAO_INCIDE (regra 6)      - tribISSQN 2 (imunidade), 3 (exportação) e 4 (não
--       incidência): Res. CGSN 140, arts. 25, § 3º, e 30;
--     ISENCAO_MUNICIPAL (regra 7)   - tpBM 1: Res. CGSN 140, art. 35, I;
--     ISS_RETIDO (regra 8)          - tpRetISSQN 2 e 3: Cosit 298/2019; LC 123, art. 21, § 4º,
--       VII; Res. CGSN 140, art. 25, § 9º, II;
--   SIPA_TIPO_ALIQUOTA: tipo de alíquota resultante (tipoAliquota na saída da API);
--   SIPA_ORDEM: número da regra no documento 07 (a ordem de avaliação das regras fica em código;
--     a coluna documenta a posição e é conferida por teste de carga).
--
-- Situações que o documento não mostra caem no par COM redistribuição (regra 9): redução da
-- base (tpBM 2 e 3), alíquota diferenciada (tpBM 4, por analogia — pendência P3), exigibilidade
-- suspensa e ISS devido a outro município sem retenção.

CREATE TABLE SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL (
    SIPA_ID INTEGER NOT NULL,
    SIPA_CAMPO TEXT NOT NULL CHECK (SIPA_CAMPO IN ('CRT', 'regApIBSCBSSN', 'regApTribSN', 'tribISSQN', 'tpBM', 'tpRetISSQN')),
    SIPA_VALOR INTEGER NOT NULL,
    SIPA_REGRA TEXT NOT NULL CHECK (SIPA_REGRA IN ('ISS_FORA_DO_SIMPLES', 'ISS_NAO_INCIDE', 'ISENCAO_MUNICIPAL', 'ISS_RETIDO')),
    SIPA_TIPO_ALIQUOTA TEXT NOT NULL CHECK (SIPA_TIPO_ALIQUOTA IN ('SEM_REDISTRIBUICAO', 'COM_REDISTRIBUICAO')),
    SIPA_ORDEM INTEGER NOT NULL CHECK (SIPA_ORDEM >= 1),
    SIPA_INICIO_VIGENCIA TEXT NOT NULL,
    SIPA_FIM_VIGENCIA TEXT DEFAULT NULL CHECK ("SIPA_FIM_VIGENCIA" IS NULL OR "SIPA_FIM_VIGENCIA" >= "SIPA_INICIO_VIGENCIA"),
    PRIMARY KEY (SIPA_ID)
);

--                                                              (id, campo, valor, regra, tipoAliquota, ordem, vigências)
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (1,  'CRT',           2, 'ISS_FORA_DO_SIMPLES', 'SEM_REDISTRIBUICAO', 4, '2027-01-01', NULL); -- excesso de sublimite (CRT)
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (2,  'regApIBSCBSSN', 2, 'ISS_FORA_DO_SIMPLES', 'SEM_REDISTRIBUICAO', 4, '2027-01-01', NULL); -- excesso de sublimite (NFS-e)
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (3,  'regApTribSN',   2, 'ISS_FORA_DO_SIMPLES', 'SEM_REDISTRIBUICAO', 4, '2027-01-01', NULL); -- federais pelo SN, ISS fora
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (4,  'regApTribSN',   3, 'ISS_FORA_DO_SIMPLES', 'SEM_REDISTRIBUICAO', 4, '2027-01-01', NULL); -- federais e ISS fora
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (5,  'tribISSQN',     2, 'ISS_NAO_INCIDE',      'SEM_REDISTRIBUICAO', 6, '2027-01-01', NULL); -- imunidade
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (6,  'tribISSQN',     3, 'ISS_NAO_INCIDE',      'SEM_REDISTRIBUICAO', 6, '2027-01-01', NULL); -- exportação de serviço
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (7,  'tribISSQN',     4, 'ISS_NAO_INCIDE',      'SEM_REDISTRIBUICAO', 6, '2027-01-01', NULL); -- não incidência
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (8,  'tpBM',          1, 'ISENCAO_MUNICIPAL',   'SEM_REDISTRIBUICAO', 7, '2027-01-01', NULL); -- isenção municipal
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (9,  'tpRetISSQN',    2, 'ISS_RETIDO',          'SEM_REDISTRIBUICAO', 8, '2027-01-01', NULL); -- retido pelo tomador
INSERT INTO SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL VALUES (10, 'tpRetISSQN',    3, 'ISS_RETIDO',          'SEM_REDISTRIBUICAO', 8, '2027-01-01', NULL); -- retido pelo intermediário
