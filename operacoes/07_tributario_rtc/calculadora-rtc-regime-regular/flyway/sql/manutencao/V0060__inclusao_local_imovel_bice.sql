-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-10-02
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-10-02'),
    'V0060',
    'Inclusão do local imóvel BICE na tabela INDICADOR_OPERACAO_IBS_CBS.'
);

-------------------------------------------------------------------------------------------------
-- Descrição:
-- Atualização do CHECK de IOIC_CD_LOCAL_FORNECIMENTO_DFE para permitir valores até 29,
-- atualização de linhas existentes e inserção de nova linha ID=66.
-------------------------------------------------------------------------------------------------

-----------------------------------------------
-- 1) Atualizar o CHECK da coluna de local DFE
-----------------------------------------------

ALTER TABLE INDICADOR_OPERACAO_IBS_CBS
    ADD COLUMN IOIC_CD_LOCAL_FORNECIMENTO_DFE_NOVO INTEGER
        CHECK (IOIC_CD_LOCAL_FORNECIMENTO_DFE_NOVO BETWEEN 1 AND 29);

UPDATE INDICADOR_OPERACAO_IBS_CBS
SET IOIC_CD_LOCAL_FORNECIMENTO_DFE_NOVO = IOIC_CD_LOCAL_FORNECIMENTO_DFE;

ALTER TABLE INDICADOR_OPERACAO_IBS_CBS
    DROP COLUMN IOIC_CD_LOCAL_FORNECIMENTO_DFE;

ALTER TABLE INDICADOR_OPERACAO_IBS_CBS
    RENAME COLUMN IOIC_CD_LOCAL_FORNECIMENTO_DFE_NOVO TO IOIC_CD_LOCAL_FORNECIMENTO_DFE;

-----------------------------------------------
-- 2) Atualizações e inserção de nova linha
-----------------------------------------------

-- Atualiza o registro ID 39 (IOIC_CD 020401): descrição e local de fornecimento DFE
UPDATE INDICADOR_OPERACAO_IBS_CBS
SET IOIC_TIPO_OPERACAO = 'Serviços de locação, sublocação, arrendamento, direito de passagem ou permissão de uso, compartilhado ou não, de ferrovia, rodovia, postes, cabos, dutos e condutos de qualquer natureza (quando caracterize operação tributável pelo ISSQN)',
    IOIC_CD_LOCAL_FORNECIMENTO_DFE = 29
WHERE IOIC_ID = 39;

-- Atualiza o registro ID 37 (IOIC_CD 020202): muda local de fornecimento DFE para 29
UPDATE INDICADOR_OPERACAO_IBS_CBS
SET IOIC_CD_LOCAL_FORNECIMENTO_DFE = 29
WHERE IOIC_ID = 37;

-- Insere nova linha ID=66, IOIC_CD='020102'
INSERT INTO INDICADOR_OPERACAO_IBS_CBS
(IOIC_ID, IOIC_CD, IOIC_LOCAL_INCIDENCIA, IOIC_INICIO_VIGENCIA, IOIC_FIM_VIGENCIA, IOIC_TIPO_OPERACAO, IOIC_CD_LOCAL_FORNECIMENTO_DFE, IOIC_CD_LOCAL_INCIDENCIA)
VALUES
(66, '020102', 'Localidade do Imóvel', '2026-11-03', NULL, 'Operação com bem imóvel, bem móvel imaterial, inclusive direito, relacionada a Bem Imóvel de Características Especiais (BICE)', 29, 6);
