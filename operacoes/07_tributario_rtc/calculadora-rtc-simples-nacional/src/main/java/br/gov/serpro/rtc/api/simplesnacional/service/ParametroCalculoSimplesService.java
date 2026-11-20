package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.simplesnacional.entity.EnquadramentoRegimeSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TransicaoAliquotaCompraGovSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegimeCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.repository.EnquadramentoRegimeSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.SituacaoIssParAliquotaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoDfeCalculoSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoReceitaBrutaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TransicaoAliquotaCompraGovSimplesRepository;
import lombok.RequiredArgsConstructor;

/**
 * Fachada de resolução dos parâmetros do cálculo do Simples Nacional que vivem
 * em banco de dados: documentos suportados e suas características de cálculo,
 * efeito dos tipos de receita bruta, enquadramento do regime e matriz de
 * regime, fator de transição de compra governamental e situações do ISS que
 * escolhem o par de alíquotas.
 *
 * <p>
 * Concentra os lookups para manter o serviço de observabilidade enxuto e para
 * ser o ponto único de um eventual cache futuro. Todos os lookups usam a DATA
 * DO FATO GERADOR da operação, nunca a data corrente.
 *
 * <p>
 * Retornos vazios significam FALHA DE PARAMETRIZAÇÃO (ou dado fora de vigência)
 * e são convertidos pelo chamador nos erros catalogados adequados (VSN-015,
 * VSN-019, VSN-025 ou VSN-026) — o cálculo nunca assume comportamento implícito.
 */
@Service
@RequiredArgsConstructor
public class ParametroCalculoSimplesService {

    private final TipoDfeCalculoSimplesRepository tipoDfeCalculoRepository;
    private final TipoReceitaBrutaSimplesRepository tipoReceitaBrutaRepository;
    private final EnquadramentoRegimeSimplesRepository enquadramentoRepository;
    private final TransicaoAliquotaCompraGovSimplesRepository transicaoCompraGovRepository;
    private final SituacaoIssParAliquotaSimplesRepository situacaoIssRepository;

    /**
     * Indica se o tipo de documento fiscal ({@code tpDoc}: 55, 65, 91, ...) é
     * suportado pelo cálculo na data do fato gerador, conforme a tabela
     * {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}.
     */
    public boolean tpDocSuportado(int tpDoc, LocalDate data) {
        return tipoDfeCalculoRepository.contarVinculosVigentes(tpDoc, data) > 0;
    }

    /**
     * Indica se o tipo de documento ({@code tpDoc}) é conhecido, mas ainda não
     * tem tratamento implementado nos endpoints de cálculo, conforme a coluna
     * {@code TPDF_B_POSSUI_TRATAMENTO_CALCULADORA} da tabela {@code TIPO_DFE}
     * vigente na data do fato gerador ({@code true} quando a coluna é 0).
     * tpDoc inexistente ⇒ {@code false}: a validação seguinte (suporte no
     * cálculo) o rejeita com VSN-015.
     */
    public boolean tpDocSemTratamentoCalculadora(int tpDoc, LocalDate data) {
        return tipoDfeCalculoRepository.contarSemTratamentoCalculadoraVigentes(tpDoc, data) > 0;
    }

    /**
     * Resolve as características do cálculo para o documento ({@code tpDoc})
     * vigentes na data do fato gerador: vocabulário do regime, casas da
     * alíquota no cálculo, máximo de itens e admissão do Imposto Seletivo.
     * Vazio = documento não suportado (VSN-015).
     */
    public Optional<TipoDfeCalculoSimples> buscarTipoDfeCalculo(int tpDoc, LocalDate data) {
        return primeiro(tipoDfeCalculoRepository.buscarVigentes(tpDoc, data));
    }

    /**
     * Resolve o efeito PADRÃO do Tipo de Receita Bruta do Simples Nacional
     * (tpRBSN) vigente na data do fato gerador — o do endpoint clássico e do
     * endpoint de atividades. Vazio = falha de parametrização (erro VSN-026).
     */
    public Optional<TipoReceitaBrutaSimples> buscarTipoReceitaBruta(int tpRBSN, LocalDate data) {
        return primeiro(tipoReceitaBrutaRepository.buscarVigentesPorCodigo(tpRBSN, data));
    }

    /**
     * Resolve o efeito do tpRBSN vigente na data do fato gerador para o
     * vocabulário do documento: a linha do vocabulário, se houver; senão, a
     * padrão. Vazio = falha de parametrização (erro VSN-026).
     */
    public Optional<TipoReceitaBrutaSimples> buscarTipoReceitaBruta(int tpRBSN, VocabularioRegimeSimplesEnum vocabulario,
            LocalDate data) {
        return primeiro(tipoReceitaBrutaRepository.buscarVigentesPorCodigoEVocabulario(tpRBSN, vocabulario, data));
    }

    /**
     * Resolve a linha da matriz de regime do endpoint CLÁSSICO, vigente na data
     * do fato gerador, pela tabela de enquadramento no vocabulário CRT
     * ({@code CRT_1} ⇒ CRT 1; {@code CRT_2} ⇒ CRT 2; {@code CRT_3_TPREGIME_2} ⇒
     * CRT 3 + tpRegimeIBSCBS 2). Vazio = falha de parametrização (erro VSN-026).
     */
    public Optional<RegimeApuracaoSimples> buscarRegimeApuracao(RegimeCalculoSimplesEnum regime, LocalDate data) {
        return switch (regime) {
            case CRT_1 -> buscarRegimeApuracao(VocabularioRegimeSimplesEnum.CRT, 1, null, null, null, data);
            case CRT_2 -> buscarRegimeApuracao(VocabularioRegimeSimplesEnum.CRT, 2, null, null, null, data);
            case CRT_3_TPREGIME_2 -> buscarRegimeApuracao(VocabularioRegimeSimplesEnum.CRT, 3, 2, null, null, data);
        };
    }

    /**
     * Resolve, pela tabela de enquadramento, o regime de apuração (código
     * neutro) da combinação de campos de regime do documento, vigente na data
     * do fato gerador. Campos do outro vocabulário devem vir nulos. Vazio =
     * combinação sem linha vigente (fora do escopo do cálculo, VSN-019, nos
     * endpoints novos).
     */
    public Optional<RegimeApuracaoSimples> buscarRegimeApuracao(VocabularioRegimeSimplesEnum vocabulario, Integer crt,
            Integer tpRegimeIBSCBS, Integer opSimpNac, Integer regApIBSCBSSN, LocalDate data) {
        return primeiro(enquadramentoRepository.buscarEnquadramentosVigentes(vocabulario, crt, tpRegimeIBSCBS,
                opSimpNac, regApIBSCBSSN, data))
                .map(EnquadramentoRegimeSimples::getRegime);
    }

    /**
     * Resolve o fator de transição de alíquotas de compra governamental (fração)
     * vigente na data do fato gerador, aplicável a ente estadual, distrital ou
     * municipal ({@code tpEnteGov} = 2, 3 ou 4). Vazio = data fora das vigências
     * (erro VSN-025; nunca assumir fator zero implícito).
     */
    public Optional<BigDecimal> buscarFatorTransicaoCompraGov(LocalDate data) {
        return primeiro(transicaoCompraGovRepository.buscarVigentes(data))
                .map(TransicaoAliquotaCompraGovSimples::getFator);
    }

    /**
     * Situações do ISS vigentes na data do fato gerador que levam ao par de
     * alíquotas sem redistribuição (regras 4, 6, 7 e 8 da escolha do par, em
     * {@code RegraAliquotaSimplesEnum}), pela ordem da regra.
     */
    public List<SituacaoIssParAliquotaSimples> buscarSituacoesIssParAliquota(LocalDate data) {
        return situacaoIssRepository.buscarVigentes(data);
    }

    /** Primeira linha da lista (vigência mais recente), ou vazio quando não há vigente. */
    private static <T> Optional<T> primeiro(List<T> vigentes) {
        return vigentes.isEmpty() ? Optional.empty() : Optional.of(vigentes.get(0));
    }
}
