-- =====================================================
-- MIGRAÇÃO DE MANUTENÇÃO
-- Data: 2026-09-15
-- =====================================================

-- *****************************************************************************************
-- ************** É OBRIGATÓRIO INCLUIR O REGISTRO NA TABELA VERSAO_BASE_DADO **************
-- *****************************************************************************************

INSERT INTO VERSAO_BASE_DADO (VRBD_DATA, VRBD_VERSAO_BASE_DADO, VRBD_DESCRICAO) VALUES
(
    datetime('2026-09-15'),
    'V0068',
    'Correção dos anexos do Fator R da atividade 09 (cAtivSN 09) do catálogo ATIVIDADE_SIMPLES_NACIONAL.'
);

-- -----------------------------------------------------------------------------------------------
-- Descrição:
-- 1. Correção dos anexos do Fator R da atividade 09 (cAtivSN 09) do catálogo ATIVIDADE_SIMPLES_NACIONAL: fator r igual ou superior a 28% = Anexo III; inferior a 28% = Anexo V (LC 123/2006, art. 18, §§ 5º-J e 5º-M; Resolução CGSN 190/2026, Anexos III e V).
-- -----------------------------------------------------------------------------------------------

-- ==========================================================================================
-- 1. Correção dos anexos do Fator R da atividade 09 (Serviços sujeitos ao Fator "R")
-- ==========================================================================================
--
-- Semântica das colunas do catálogo:
--   ATSN_ANEXO_COM_FATOR_R = anexo aplicável quando o fator "r" é ATINGIDO (r >= 28%);
--   ATSN_ANEXO_SEM_FATOR_R = anexo aplicável quando o fator "r" NÃO é atingido (r < 28%),
--                            ou quando a atividade não depende do fator "r".
--
-- Base legal:
--   - LC 123/2006, art. 18, § 5º-J: as atividades de prestação de serviços do § 5º-I serão
--     tributadas na forma do Anexo III quando o fator "r" for igual ou superior a 28%;
--   - LC 123/2006, art. 18, § 5º-M: quando o fator "r" for inferior a 28%, serão tributadas
--     na forma do Anexo V;
--   - Resolução CGSN nº 190, de 4 de agosto de 2026: cabeçalho do Anexo III ("... serviços
--     descritos no inciso V quando o fator 'r' for igual ou superior a 28%") e do Anexo V
--     ("... serviços descritos no inciso V do § 1º do art. 25, quando o fator 'r' for
--     inferior a 28%").
--
-- A carga da V0049 gravou a atividade 09 com os anexos INVERTIDOS (SEM_FATOR_R = 'III' e
-- COM_FATOR_R = 'V'). Esta migração corrige o dado: COM_FATOR_R = 'III' e SEM_FATOR_R = 'V'.
--
-- Por que UPDATE, e não versionamento de vigência (fechar ATSN_FIM_VIGENCIA e criar linha
-- nova), como recomenda o cabeçalho da V0049 para correções de numeração ou descrição:
-- trata-se de correção de um ERRO DE CARGA — o valor invertido nunca correspondeu à lei em
-- vigência alguma, a vigência do catálogo (2027-01-01) ainda não começou e não há recálculo
-- retroativo a preservar. Uma linha nova com vigência própria deixaria o dado errado válido
-- para um período em que ele jamais foi correto.
--
-- Os rótulos ATSN_ROTULO/ATSN_ROTULO_CURTO ("III ou V") permanecem como estão: a ordem de
-- exibição dos anexos segue DerivacaoAnexosSimples.ANEXOS_VALIDOS (I..V), não as colunas.
--
-- A regra de derivação de modoFatorR (DerivacaoAnexosSimples) NÃO muda: só os valores e os
-- textos. O consumidor que já resolvia "fatorR = true => anexoComFatorR" passa a obter o
-- Anexo III sem nenhuma alteração de código.

UPDATE ATIVIDADE_SIMPLES_NACIONAL
SET    ATSN_ANEXO_SEM_FATOR_R = 'V',
       ATSN_ANEXO_COM_FATOR_R = 'III'
WHERE  ATSN_CATIVSN = '09';
