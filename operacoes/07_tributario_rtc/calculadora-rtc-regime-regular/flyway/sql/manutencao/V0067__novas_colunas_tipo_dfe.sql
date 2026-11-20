-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-09-23
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-09-23'),
    'V0067',
    'Novas colunas na tabela TIPO_DFE'
);

-- Alterar a coluna TIPO_DFE.TPDF_TIPO para ser NOT NULL
ALTER TABLE TIPO_DFE ADD COLUMN TPDF_TIPO_NOT_NULL INTEGER NOT NULL DEFAULT 0; -- Criar uma nova coluna temporária para armazenar os valores existentes

-- Estamos assumindo aqui que os valores da coluna TPDF_TIPO já estão preenchidos corretamente e não contêm valores nulos. Atualmente está assim mesmo.  
UPDATE TIPO_DFE SET TPDF_TIPO_NOT_NULL = TPDF_TIPO; -- Copiar os valores da coluna TPDF_TIPO para a nova coluna temporária
ALTER TABLE TIPO_DFE DROP COLUMN TPDF_TIPO; -- Remover a coluna TPDF_TIPO original
ALTER TABLE TIPO_DFE RENAME COLUMN TPDF_TIPO_NOT_NULL TO TPDF_TIPO; -- Renomear a coluna temporária para TPDF_TIPO

-- Adicionar coluna TPDF_PERMITE_NCM_ZERO na tabela TIPO_DFE
ALTER TABLE TIPO_DFE ADD COLUMN TPDF_B_PERMITE_NCM_ZERO BOOLEAN NOT NULL DEFAULT 0;
-- Atualizar a coluna TPDF_PERMITE_NCM_ZERO para 1 nos tipos de DFE 55 (NF-e) e 65 (NFC-e)
UPDATE TIPO_DFE SET TPDF_B_PERMITE_NCM_ZERO = 1 WHERE TPDF_TIPO IN (55, 65);

-- Adicionar coluna TPDF_POSSUI_TRATAMENTO_CALCULADORA na tabela TIPO_DFE
ALTER TABLE TIPO_DFE ADD COLUMN TPDF_B_POSSUI_TRATAMENTO_CALCULADORA BOOLEAN NOT NULL DEFAULT 1;
-- Atualizar a coluna TPDF_POSSUI_TRATAMENTO_CALCULADORA para 0 nos tipos de DFE 77	(NF-e ABI), 94 (DERE), 95 (DIR) e 96 (Duimp)
UPDATE TIPO_DFE SET TPDF_B_POSSUI_TRATAMENTO_CALCULADORA = 0 WHERE TPDF_TIPO IN (77, 94, 95, 96);