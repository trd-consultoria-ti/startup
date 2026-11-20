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
    'V0064',
    'Inclui classificação omitida na atualização da tabela CLASSIF_NBS_INDOP_LC.'
);

-- Correção de typo no material de origem: cIndOp 100303 corresponde a 100301 (IOIC_ID 55).
INSERT INTO CLASSIF_NBS_INDOP_LC
    (CNIL_ID, CNIL_LSLC_CD, CNIL_NBS_CD, CNIL_IOIC_ID, CNIL_CLTR_ID,
     CNIL_INICIO_VIGENCIA, CNIL_FIM_VIGENCIA, CNIL_IN_PS_ONEROSA, CNIL_IN_ADQ_EXTERIOR)
VALUES
    (3284, '20.02', '106062000', 55, 1, '2026-11-03', NULL, 1, 0);
