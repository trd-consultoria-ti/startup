-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-10-21
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-10-21'),
    'V0070',
    'Atualiza a tabela NCM aplicando as vigências conforme o sistema Classif'
);

-- -----------------------------------------------------------------------------------------------
-- Descrição:
-- Atualiza a tabela NCM aplicando as vigências conforme o sistema Classif
-- -----------------------------------------------------------------------------------------------

UPDATE NCM SET NCM_DESCRICAO = 'Para inspeção visual de tecidos', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00' 
 WHERE NCM_CD = '84515010';
 
UPDATE NCM SET NCM_DESCRICAO = 'De máquinas dos itens 8450.20.10 ou 8450.20.90', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00'
 WHERE NCM_CD = '84509010';

UPDATE NCM SET NCM_DESCRICAO = 'Forato (ISO); terbufós (ISO)', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00' 
 WHERE NCM_CD = '29309051';
 
UPDATE NCM SET NCM_DESCRICAO = 'Amprenavir; aprepitanto; delavirdina ou seu mesilato; efavirenz; emtricitabina; etopósido; everolimus; fosamprenavir cálcico; fosfato de fludarabina; gencitabina ou seu cloridrato; mesilato de dabrafenibe; raltitrexida; ritonavir; sirolimus; tacrolimus; temsirolimus; tenipósido; tipranavir', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00' 
 WHERE NCM_CD = '30039088';

UPDATE NCM SET NCM_DESCRICAO = 'Amprenavir; aprepitanto; delavirdina ou seu mesilato; efavirenz; emtricitabina; etopósido; everolimus; fosamprenavir cálcico; fosfato de fludarabina; gencitabina ou seu cloridrato; mesilato de dabrafenibe; raltitrexida; ritonavir; sirolimus; tacrolimus; temsirolimus; tenipósido; tipranavir', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00' 
 WHERE NCM_CD = '30049078';

UPDATE NCM SET NCM_DESCRICAO = 'Quitosana, seus sais ou seus derivados', 
			   NCM_INICIO_VIGENCIA = '2026-02-01T00:00:00' 
 WHERE NCM_CD = '39139050';

INSERT INTO NCM (NCM_CD,NCM_DESCRICAO,NCM_INICIO_VIGENCIA,NCM_FIM_VIGENCIA) VALUES ('85181020','Outros microfones, sem suporte','2026-10-01T00:00:00','9999-12-31T00:00:00');

