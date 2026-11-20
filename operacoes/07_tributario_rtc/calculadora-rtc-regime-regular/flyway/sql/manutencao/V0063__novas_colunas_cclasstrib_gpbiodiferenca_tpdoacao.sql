-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-10-07
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-10-07'),
    'V0063',
    'Indicador do grupo gpBioDiferenca (ind_gpBioDiferenca) e tipo de doação (tpDoacao) na tabela de Classificações Tributárias.'
);

-- -----------------------------------------------------------------------------------------------
-- Descrição:
-- Inclusão e carga das colunas CLTR_IND_GPBIODIFERENCA e CLTR_TPDOACAO na tabela
-- CLASSIFICACAO_TRIBUTARIA, conforme planilha "IT 2025.002 v.1.70 - cClassTrib.xlsx"
-- (aba "cClass 2026-09-01", colunas ind_gpBioDiferenca e tpDoacao - 173 códigos),
-- RESTRITA às classificações do tributo CBS (TBTO_ID = 2).
--
-- IMPORTANTE - CLTR_CD NÃO É ÚNICO: a tabela CLASSIFICACAO_TRIBUTARIA contém DOIS grupos
-- de classificações, um do Imposto Seletivo (TBTO_ID = 1) e outro da CBS (TBTO_ID = 2),
-- com códigos que podem colidir entre os grupos. As duas colunas pertencem exclusivamente
-- à tabela cClassTrib do IBS/CBS; por isso, TODOS os UPDATEs abaixo restringem o alvo via
-- EXISTS em TRIBUTO_SITUACAO_TRIBUTARIA com TRST_TBTO_ID = 2 (CBS).
-- As classificações do Imposto Seletivo permanecem no DEFAULT 0.
--
-- O DEFAULT 0 existe também para viabilizar o ALTER no SQLite. Códigos futuros DEVEM ser
-- carregados com os valores corretos das duas colunas.
-- -----------------------------------------------------------------------------------------------

-- ==========================================================================================
-- 1. Inclusão da coluna CLTR_IND_GPBIODIFERENCA e carga
-- ==========================================================================================
--
-- Indicador do grupo gpBioDiferenca (ind_gpBioDiferenca) do cClassTrib:
--   0 = O grupo gpBioDiferenca não se aplica ao cClassTrib
--   1 = O grupo gpBioDiferenca se aplica ao cClassTrib

ALTER TABLE CLASSIFICACAO_TRIBUTARIA
    ADD COLUMN CLTR_IND_GPBIODIFERENCA INTEGER NOT NULL DEFAULT 0
        CHECK (CLTR_IND_GPBIODIFERENCA IN (0, 1));

-- ind_gpBioDiferenca = 1 (2 códigos); os demais 171 códigos permanecem com o DEFAULT 0

UPDATE CLASSIFICACAO_TRIBUTARIA
   SET CLTR_IND_GPBIODIFERENCA = 1
 WHERE CLTR_CD IN (
    '620004','620005'
 )
   AND EXISTS (
       SELECT 1
         FROM TRIBUTO_SITUACAO_TRIBUTARIA TST
        WHERE TST.TRST_SITR_ID = CLASSIFICACAO_TRIBUTARIA.CLTR_SITR_ID
          AND TST.TRST_TBTO_ID = 2
   );

-- ==========================================================================================
-- 2. Inclusão da coluna CLTR_TPDOACAO e carga
-- ==========================================================================================
--
-- Tipo de doação (tpDoacao) do cClassTrib, conforme aba "tpDoacao" da planilha:
--   0 = Campo indDoacao não pode ser preenchido
--   1 = Campo indDoacao deve obrigatoriamente ser preenchido
--   2 = Campo indDoacao deve ser preenchido quando a doação for tributada conforme Art. 6º, 2º, I

ALTER TABLE CLASSIFICACAO_TRIBUTARIA
    ADD COLUMN CLTR_TPDOACAO INTEGER NOT NULL DEFAULT 0
        CHECK (CLTR_TPDOACAO IN (0, 1, 2));

-- tpDoacao = 0 - Campo indDoacao não pode ser preenchido (130 códigos): permanecem com o DEFAULT 0

-- tpDoacao = 1 - Campo indDoacao deve obrigatoriamente ser preenchido (2 códigos)

UPDATE CLASSIFICACAO_TRIBUTARIA
   SET CLTR_TPDOACAO = 1
 WHERE CLTR_CD IN (
    '410003','410026'
 )
   AND EXISTS (
       SELECT 1
         FROM TRIBUTO_SITUACAO_TRIBUTARIA TST
        WHERE TST.TRST_SITR_ID = CLASSIFICACAO_TRIBUTARIA.CLTR_SITR_ID
          AND TST.TRST_TBTO_ID = 2
   );

-- tpDoacao = 2 - Campo indDoacao deve ser preenchido quando a doação for tributada (41 códigos)

UPDATE CLASSIFICACAO_TRIBUTARIA
   SET CLTR_TPDOACAO = 2
 WHERE CLTR_CD IN (
    '000001','000005','000006','010001','010002','011001','011002','011003',
    '011004','011005','200025','200026','200027','200028','200029','200030',
    '200031','200032','200033','200034','200035','200036','200037','200038',
    '200039','200040','200041','200042','200043','200044','200045','200046',
    '200047','200048','200049','200050','200051','200052','200055','200056',
    '222001'
 )
   AND EXISTS (
       SELECT 1
         FROM TRIBUTO_SITUACAO_TRIBUTARIA TST
        WHERE TST.TRST_SITR_ID = CLASSIFICACAO_TRIBUTARIA.CLTR_SITR_ID
          AND TST.TRST_TBTO_ID = 2
   );
