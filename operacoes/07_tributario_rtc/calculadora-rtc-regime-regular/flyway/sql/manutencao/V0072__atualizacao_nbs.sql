-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-11-04
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-11-04'),
    'V0072',
    'Atualiza a tabela NBS conforme arquivo nbs2-0.csv disponibilizado na base MDIC'
);

-- -----------------------------------------------------------------------------------------------
-- Descrição:
-- Atualiza a tabela NBS conforme arquivo nbs2-0.csv disponibilizado na base MDIC
-- -----------------------------------------------------------------------------------------------

UPDATE NBS SET NBS_CD = '10910'
WHERE NBS_CD = '1091';

UPDATE NBS SET NBS_CD = '110'
WHERE NBS_CD = '11';

UPDATE NBS SET NBS_CD = '120'
WHERE NBS_CD = '12';

UPDATE NBS SET NBS_CD = '11410'
WHERE NBS_CD = '1141';

