-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-10-28
-- =====================================================

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-10-28'),
    'V0066',
    'Exclui classificação indevida da tabela CLASSIF_NBS_INDOP_LC.'
);

DELETE FROM CLASSIF_NBS_INDOP_LC WHERE CNIL_ID = 3087;
