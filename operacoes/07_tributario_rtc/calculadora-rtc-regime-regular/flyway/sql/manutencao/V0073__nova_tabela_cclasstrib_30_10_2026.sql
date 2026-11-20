-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-11-09
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-11-09'),
    'V0073',
    'Nova tabela de Classificações Tributárias (IT 2025.002 v.1.71), publicada em 30/10/2026.'
);

-------------------------------------------------------------------------------------------------
-- Descrição:
-- 1. Atualização da tabela de cClassTrib conforme planilha "IT 2025.002 v.1.71 - cClassTrib.xlsx"
--    (publicada no Portal da NF-e em 30/10/2026; células verdes da aba "cClass 2026-09-01",
--    DataAtualização 08/10/2026):
--      - 410003: novo nome ("amostras grátis") e inclusão em indDIR;
--      - 410026: novo nome ("amostras grátis");
--      - 410999: inclusão em indDIR;
--      - data de atualização (08/10/2026) das três classificações.
--
-- 2. Remoção da exceção do NCM 0710.80.00 na classificação 200014 (Anexo XV, item 2).
--
-- Observações:
--   - CLTR_DESCRICAO armazena o "Nome cClassTrib" (coluna D). A coluna "Descrição cClassTrib"
--     (coluna E) da planilha não é carregada.
--   - As abas CST, tpDoacao e tpRBSN não possuem alterações (sem células verdes).
--   - CLTR_CD não é único (os códigos do Imposto Seletivo colidem com os do IBS/CBS): os alvos
--     são identificados por CLTR_ID, com CLTR_CD como trava de segurança.
-------------------------------------------------------------------------------------------------

-- ==============================================
-- 1.1. Nome das Classificações Tributárias
-- ==============================================
-- 410003
UPDATE CLASSIFICACAO_TRIBUTARIA
SET CLTR_DESCRICAO = 'Doações e amostras grátis sem contraprestação em benefício do doador'
WHERE CLTR_ID = 72 AND CLTR_CD = '410003';

-- 410026
UPDATE CLASSIFICACAO_TRIBUTARIA
SET CLTR_DESCRICAO = 'Doação e amostras grátis com anulação de crédito'
WHERE CLTR_ID = 95 AND CLTR_CD = '410026';

-- ========================================================
-- 1.2. Ajustes na tabela associativa TIPO_DFE_CLASSIFICACAO
-- ========================================================
INSERT INTO TIPO_DFE_CLASSIFICACAO
    (TDCL_ID, TDCL_CLTR_ID, TDCL_TPDF_ID, TDCL_INICIO_VIGENCIA, TDCL_FIM_VIGENCIA)
VALUES
    (446, 72, 17, '2026-01-01', NULL),  -- 410003: indDIR
    (447, 96, 17, '2026-01-01', NULL);  -- 410999: indDIR

-- ==============================================
-- 1.3. Data de atualização
-- ==============================================
UPDATE CLASSIFICACAO_TRIBUTARIA
SET CLTR_DATA_ATUALIZACAO = '2026-10-08'
WHERE (CLTR_ID, CLTR_CD) IN (
    (72, '410003'),
    (95, '410026'),
    (96, '410999')
);

-- ===================================================================================
-- 2. Remoção da exceção do NCM 0710.80.00 na classificação 200014 (Anexo XV, item 2)
-- ===================================================================================
-- LC 214/2025, Anexo XV, item 2: "Produtos hortícolas das posições 07.01, ..., 07.09 e 07.10,
-- exceto os cogumelos e trufas classificados na subposição 0709.5 e no código 0710.80.00".
--
-- A exclusão alcança apenas os cogumelos e trufas. A subposição 0709.5 contém somente
-- cogumelos e trufas, por isso a exceção 07095 (ENCM_ID = 66) continua válida. Já o código
-- 0710.80.00 ("Outros produtos hortícolas", congelados) também abrange outros produtos
-- hortícolas, que têm direito à redução a zero. O NCM sozinho não diferencia esses casos,
-- então 0710.80.00 deve ser aceito na 200014.
--
-- Decisão de negócio de 25/06/2026: o NCM 0710.80.00 deve ser aceito na 200014
-- (ex.: brócolis congelado). A V0037 tentou desativar a exceção fechando a vigência com
-- ENCM_FIM_VIGENCIA = ENCM_INICIO_VIGENCIA = 2026-01-01. Como a vigência é um intervalo fechado
-- (BETWEEN, o último dia é vigente), a exceção continuou valendo em 01/01/2026. Para um registro
-- que nunca deveria ter existido, não há como fechar a vigência sem inventar datas (o CHECK
-- exige fim >= início). Por isso o registro é removido.
DELETE FROM EXCECAO_NCM_APLICAVEL
WHERE ENCM_ID = 67
  AND ENCM_NCM_CD = '07108000'
  AND ENCM_NCMA_ID = 984;
