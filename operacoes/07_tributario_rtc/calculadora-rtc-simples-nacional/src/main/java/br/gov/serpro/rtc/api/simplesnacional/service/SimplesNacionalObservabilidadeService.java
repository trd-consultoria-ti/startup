package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.simplesnacional.adapter.ValidacaoNomenclaturaAdapter;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegimeCalculoSimplesEnum;
import br.gov.serpro.rtc.shared.exceptionhandler.ErrosCalculoException;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.model.input.ItemSimplesInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.OperacaoSimplesInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObjetoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObservabilidadeSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.OperacaoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.TotalSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.repository.AtividadeSimplesCalculoRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoDfeCalculoSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.AliquotasItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ContextoItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.EntradaItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ItemAProcessar;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ItemProcessadoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ResolvedorAliquotasItem;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Serviço de observabilidade do Simples Nacional (NT 2026.005).
 * <p>
 * Duplicado deliberadamente do análogo {@code ObservabilidadeService} do antigo
 * regime geral (removido do projeto) para
 * preservar o isolamento entre os pacotes. Responsabilidades:
 * <ul>
 * <li>validação global de escopo (tpDoc, CRT, tpRegimeIBSCBS, tpEnteGov) — HTTP
 * 422; o tpEnteGov é global porque o grupo {@code gCompraGov} pertence à
 * identificação da nota (grupo B/ide do leiaute do DF-e);</li>
 * <li>verificação PRIORITÁRIA, por item, de cClassTrib ainda não implementado
 * na Calculadora — primeira consulta de banco do item; indicador de ajuste do
 * tratamento tributário vigente ({@code TRTR_IN_POSSUI_AJUSTE}) → estado
 * {@code NAO_IMPLEMENTADO} (VSN-027), sem outras validações nem cálculo;</li>
 * <li>resolução, por item, do tipo de receita bruta pelo cClassTrib e validação
 * do código de atividade (cAtivSN) — feitas no {@link ProcessadorItemSimples},
 * compartilhado com os endpoints não assistido e assistido;</li>
 * <li>exigência condicional das alíquotas efetivas (apenas quando o cálculo as
 * usa), lidas dos campos {@code pIBSSN}/{@code pCBSSN} de cada item;</li>
 * <li>resolução dos parâmetros de cálculo em banco (via
 * {@link ParametroCalculoSimplesService}) e entrega de insumos prontos ao
 * serviço de cálculo, que é puro;</li>
 * <li>processamento paralelo com virtual threads;</li>
 * <li>acúmulo de erros RFC 7807 por item;</li>
 * <li>totalização: itens {@code CALCULADO} em todos os totais;
 * {@code NAO_CALCULADO} apenas em {@code vRBSNtot}; itens com erro ou
 * {@code NAO_IMPLEMENTADO} em nada.</li>
 * </ul>
 * A validação de nomenclatura reutiliza os serviços do regime geral; o cálculo
 * do Simples permanece separado das regras de cálculo daquele regime.
 *
 */
@Slf4j
@Service
public class SimplesNacionalObservabilidadeService {

    /** Nomes de campos do contrato usados em JSON Pointers e mensagens. */
    private static final String CAMPO_TPREGIMEIBSCBS = "tpRegimeIBSCBS";
    private static final String CAMPO_CRT = "CRT";
    private static final String CAMPO_TPENTEGOV = "tpEnteGov";

    private final ParametroCalculoSimplesService parametroService;
    private final ProcessadorItemSimples processadorItem;
    private final TipoDfeCalculoSimplesRepository tipoDfeRepository;

    public SimplesNacionalObservabilidadeService(SimplesNacionalCalculoService calculoService,
            ParametroCalculoSimplesService parametroService,
            ClassificacaoTributariaSimplesRepository classificacaoTributariaRepository,
            AtividadeSimplesCalculoRepository atividadeCalculoRepository,
            ValidacaoNomenclaturaAdapter validacaoNomenclaturaSimples,
            TipoDfeCalculoSimplesRepository tipoDfeRepository) {
        this.parametroService = parametroService;
        this.tipoDfeRepository = tipoDfeRepository;
        this.processadorItem = new ProcessadorItemSimples(calculoService, parametroService,
                classificacaoTributariaRepository, atividadeCalculoRepository, validacaoNomenclaturaSimples);
    }

    public ObservabilidadeSimplesDomain processarOperacao(OperacaoSimplesInput operacao, String baseURL) {
        // 1. Validar campos globais e escopo — se falhar, lança exceção (HTTP 422).
        List<ErroDetalhe> errosGlobais = validarCamposGlobais(operacao, baseURL);
        if (!errosGlobais.isEmpty()) {
            throw new ErrosCalculoException(errosGlobais);
        }

        final LocalDate data = LocalDate.parse(operacao.data().trim());

        // 2. Resolver a linha vigente da matriz de regime. Combinação aprovada
        // pela validação de escopo mas sem linha vigente na tabela é falha de
        // parametrização (VSN-026), global porque o regime vale para a operação
        // inteira.
        final RegimeApuracaoSimples regime = parametroService
                .buscarRegimeApuracao(resolverRegime(operacao), data)
                .orElseThrow(() -> new ErrosCalculoException(List.of(buildErroGlobal(
                        CodigoValidacaoSimples.PARAMETRIZACAO_INDISPONIVEL, CAMPO_CRT,
                        String.format("Não há parametrização de regime de apuração do Simples Nacional vigente "
                                + "para CRT=%s e tpRegimeIBSCBS=%s na data '%s'.",
                                operacao.CRT(), operacao.tpRegimeIBSCBS(), data),
                        baseURL))));

        // 3. Resolver o fator de transição de compra governamental — global,
        // como o tpEnteGov (a validação de domínio já ocorreu no passo 1).
        // Exigido apenas para ente estadual/distrital/municipal (2, 3 ou 4); a
        // União (1) tem regra estrutural sem parâmetro temporal. Data sem
        // vigência na tabela de transição é falha de parametrização (VSN-025) —
        // nunca assumir fator zero implícito.
        final Integer tpEnteGov = parseInteiro(operacao.tpEnteGov());
        BigDecimal fatorTransicao = null;
        if (tpEnteGov != null && tpEnteGov != 1) {
            fatorTransicao = parametroService.buscarFatorTransicaoCompraGov(data)
                    .orElseThrow(() -> new ErrosCalculoException(List.of(buildErroGlobal(
                            CodigoValidacaoSimples.COMPRA_GOV_SEM_VIGENCIA, CAMPO_TPENTEGOV,
                            String.format("Não há parametrização de transição de compra governamental vigente "
                                    + "para a data '%s'.", data),
                            baseURL))));
        }

        // O clássico mantém o comportamento de sempre para qualquer tpDoc: linha
        // padrão do tpRBSN, cAtivSN como recebido, Imposto Seletivo exigido e
        // alíquota com a precisão integral.
        final int tpDoc = Integer.parseInt(operacao.tpDoc().trim());
        // Resolve a mesma linha habilitada usada pelo parâmetro do documento e busca sua sigla por TPDF_ID.
        final var documento = parametroService.buscarTipoDfeCalculo(tpDoc, data).orElse(null);
        final String siglaDfe = documento == null ? null
                : tipoDfeRepository.buscarSiglaVigentePorId(documento.getTipoDfeId(), data).orElse(null);
        final boolean permiteNcmZero = documento != null
                && tipoDfeRepository.permiteNcmZeroVigentePorId(documento.getTipoDfeId(), data).orElse(false);
        final ContextoItemSimples contexto = ContextoItemSimples.classico(
                data, tpDoc, regime, tpEnteGov, fatorTransicao, siglaDfe, permiteNcmZero);
        final List<ItemAProcessar> itens = operacao.itens().stream()
                .map(item -> new ItemAProcessar(new EntradaItemSimples(item.numero(), item.cAtivSN(),
                        item.cClassTrib(), item.baseCalculo(), item.valorImpostoSeletivo(), item.NCM(), item.NBS()), aliquotasDoItem(item)))
                .toList();

        // 4. Processar itens em paralelo com virtual threads (processamento por
        // item compartilhado com os endpoints não assistido e assistido).
        List<ObjetoSimplesDomain> resultados = processadorItem.processarItens(itens, contexto, baseURL).stream()
                .map(ItemProcessadoSimples::objeto)
                .sorted(Comparator.comparing(ObjetoSimplesDomain::nObj,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        // 5. Totalizar: CALCULADO e NAO_CALCULADO possuem tribCalc (o segundo
        // contribui apenas para vRBSNtot); itens com erro ou NAO_IMPLEMENTADO
        // não contribuem.
        return new ObservabilidadeSimplesDomain(
                new OperacaoSimplesDomain(operacao.data(), operacao.tpDoc(), operacao.CRT(),
                        operacao.tpRegimeIBSCBS(), operacao.tpEnteGov()),
                resultados,
                TotalSimplesDomain.totalizar(resultados));
    }

    // ─── Alíquotas do item (contrato clássico) ─────────────────────────────────

    /**
     * Resolvedor de alíquotas do endpoint clássico: as alíquotas efetivas vêm
     * dos campos {@code pIBSSN}/{@code pCBSSN} do próprio item (pontos
     * percentuais, texto). A ausência só é erro quando o cálculo as usa;
     * formato inválido é sempre erro (VSN-005/VSN-007).
     */
    private static ResolvedorAliquotasItem aliquotasDoItem(ItemSimplesInput item) {
        return (dados, collector) -> {
            BigDecimal pCbs = ProcessadorItemSimples.validarAliquota(item.pCBSSN(), dados.cbsExigida(), collector,
                    "pCBSSN", CodigoValidacaoSimples.ALIQUOTA_CBS_AUSENTE, CodigoValidacaoSimples.ALIQUOTA_CBS_INVALIDA);
            BigDecimal pIbs = ProcessadorItemSimples.validarAliquota(item.pIBSSN(), dados.ibsExigida(), collector,
                    "pIBSSN", CodigoValidacaoSimples.ALIQUOTA_IBS_AUSENTE, CodigoValidacaoSimples.ALIQUOTA_IBS_INVALIDA);
            return new AliquotasItemSimples(pIbs, pCbs, false, null);
        };
    }

    // ─── Validação global da operação ──────────────────────────────────────────

    private List<ErroDetalhe> validarCamposGlobais(OperacaoSimplesInput operacao, String baseURL) {
        List<ErroDetalhe> erros = new ArrayList<>();

        if (operacao.id() == null || operacao.id().isBlank()) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.DADO_GLOBAL_AUSENTE, "id",
                    "O identificador da operação (id) é obrigatório.", baseURL));
        }
        if (operacao.versao() == null || operacao.versao().isBlank()) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.DADO_GLOBAL_AUSENTE, "versao",
                    "A versão do payload (versao) é obrigatória.", baseURL));
        }

        // A data é validada antes do escopo porque a verificação de suporte do
        // tpDoc é um lookup de vigência que depende dela.
        LocalDate data = validarData(operacao.data(), erros, baseURL);
        validarEscopo(operacao, data, erros, baseURL);
        validarTpEnteGov(operacao.tpEnteGov(), erros, baseURL);

        if (operacao.itens() == null || operacao.itens().isEmpty()) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.ITENS_VAZIOS, "itens",
                    "A lista de itens é obrigatória e não pode estar vazia.", baseURL));
        }
        return erros;
    }

    /**
     *
     * Validação global de escopo do cálculo (NT 2026.005; regras C21a-10/20/30,
     * msg 1275). Qualquer falha rejeita a operação inteira com HTTP 422.
     *
     * <p>Combinações que CALCULAM: CRT=1 (sem tpRegimeIBSCBS), CRT=2 (sem
     * tpRegimeIBSCBS) e CRT=3 + tpRegimeIBSCBS=2. Todo o resto é rejeitado:
     * tpDoc ausente/não suportado, CRT ausente/inválido, MEI (CRT=4),
     * regime regular (tpRegimeIBSCBS=1), regime normal puro (CRT=3 sem
     * tpRegimeIBSCBS=2) e as incoerências CRT × tpRegimeIBSCBS.
     *
     * <p>O suporte do tpDoc é parametrizado na tabela
     * {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}, pela vigência da data do fato
     * gerador; quando a data é inválida (já reportada como VSN-012), a
     * verificação de suporte é omitida.
     *
     */
    private void validarEscopo(OperacaoSimplesInput operacao, LocalDate data,
            List<ErroDetalhe> erros, String baseURL) {
        // tpDoc: obrigatório e suportado pelo cálculo na data do fato gerador.
        if (operacao.tpDoc() == null || operacao.tpDoc().isBlank()) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.TPDOC_AUSENTE, "tpDoc",
                    "O tipo de documento fiscal (tpDoc) é obrigatório e não foi informado.", baseURL));
        } else {
            Integer tpDoc = parseInteiro(operacao.tpDoc());
            if (tpDoc == null) {
                erros.add(buildErroGlobal(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, "tpDoc",
                        String.format("Tipo de documento não suportado pelo cálculo do Simples Nacional, "
                                + "recebido: '%s'.", operacao.tpDoc()), baseURL));
            } else if (data != null && parametroService.tpDocSemTratamentoCalculadora(tpDoc, data)) {
                erros.add(buildErroGlobal(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, "tpDoc",
                        String.format("Tipo de documento fiscal (tpDoc) de código %d ainda não possui "
                                + "tratamento implementado no cálculo do Simples Nacional.", tpDoc), baseURL));
            } else if (data != null && !parametroService.tpDocSuportado(tpDoc, data)) {
                erros.add(buildErroGlobal(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, "tpDoc",
                        String.format("Tipo de documento não suportado pelo cálculo do Simples Nacional, "
                                + "recebido: '%s'.", operacao.tpDoc()), baseURL));
            }
        }

        // CRT: obrigatório, em {1, 2, 3, 4}.
        Integer crt = parseInteiro(operacao.CRT());
        if (crt == null || crt < 1 || crt > 4) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.CRT_INVALIDO, CAMPO_CRT,
                    String.format("O Código de Regime Tributário (CRT) é obrigatório e deve ser 1, 2, 3 ou 4, "
                            + "recebido: '%s'.", operacao.CRT()), baseURL));
            return;
        }

        // tpRegimeIBSCBS: opcional; quando informado, em {1, 2}.
        boolean tpRegimeInformado = operacao.tpRegimeIBSCBS() != null && !operacao.tpRegimeIBSCBS().isBlank();
        Integer tpRegime = null;
        if (tpRegimeInformado) {
            tpRegime = parseInteiro(operacao.tpRegimeIBSCBS());
            if (tpRegime == null || (tpRegime != 1 && tpRegime != 2)) {
                erros.add(buildErroGlobal(CodigoValidacaoSimples.TPREGIMEIBSCBS_INVALIDO, CAMPO_TPREGIMEIBSCBS,
                        String.format("Tipo de regime de apuração do IBS/CBS inválido, recebido: '%s' "
                                + "(esperado 1 ou 2).", operacao.tpRegimeIBSCBS()), baseURL));
                return;
            }
        }

        // Incoerências CRT × tpRegimeIBSCBS (regras C21a-10/20/30 da NT, msg 1275).
        if (crt == 4 && tpRegimeInformado) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    "CRT=4 (MEI) não admite tpRegimeIBSCBS algum (regra C21a-30 da NT 2026.005).", baseURL));
        } else if ((crt == 1 || crt == 2) && Integer.valueOf(2).equals(tpRegime)) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    String.format("CRT=%d não admite tpRegimeIBSCBS=2 (regra C21a-10 da NT 2026.005).", crt),
                    baseURL));
        } else if (crt == 3 && Integer.valueOf(1).equals(tpRegime)) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    "CRT=3 não admite tpRegimeIBSCBS=1 (regra C21a-20 da NT 2026.005).", baseURL));
        }

        // Fora do escopo do cálculo do Simples Nacional.
        if (crt == 4) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.OPERACAO_FORA_ESCOPO, CAMPO_CRT,
                    "Operação fora do escopo do cálculo do Simples Nacional: MEI (CRT=4) "
                            + "está fora do Evento de Tributação do Simples Nacional.", baseURL));
        }
        if (Integer.valueOf(1).equals(tpRegime)) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.OPERACAO_FORA_ESCOPO, CAMPO_TPREGIMEIBSCBS,
                    "Operação fora do escopo do cálculo do Simples Nacional: optante pelo regime "
                            + "regular do IBS/CBS (tpRegimeIBSCBS=1) tem a tributação calculada pelo regime "
                            + "regular, não por este endpoint.", baseURL));
        }
        if (crt == 3 && !tpRegimeInformado) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.OPERACAO_FORA_ESCOPO, CAMPO_CRT,
                    "Operação fora do escopo do cálculo do Simples Nacional: CRT=3 sem "
                            + "tpRegimeIBSCBS=2 (regime normal puro) não é Simples Nacional.", baseURL));
        }
    }

    /**
     * Validação GLOBAL do tipo de ente governamental (compra governamental),
     * quando informado: deve ser 1 (União), 2 (Estado), 3 (DF) ou 4 (Município).
     *
     * <p>O campo é da OPERAÇÃO — espelha a tag {@code gCompraGov/tpEnteGov} da
     * identificação da nota (grupo B/ide do leiaute do DF-e, NT 2025.002-RTC):
     * o ente público adquirente é o destinatário do documento e não varia por
     * item. Valor fora do domínio rejeita a operação inteira (VSN-024,
     * HTTP 422).
     */
    private void validarTpEnteGov(String tpEnteGov, List<ErroDetalhe> erros, String baseURL) {
        if (tpEnteGov == null || tpEnteGov.isBlank()) {
            return;
        }
        Integer valor = parseInteiro(tpEnteGov);
        if (valor == null || valor < 1 || valor > 4) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.TPENTEGOV_INVALIDO, CAMPO_TPENTEGOV,
                    String.format("Tipo de ente governamental inválido, recebido: '%s' (esperado 1, 2, 3 ou 4).",
                            tpEnteGov), baseURL));
        }
    }

    /**
     * Resolve o regime de cálculo a partir de uma operação JÁ APROVADA pela
     * validação de escopo (as combinações restantes são exatamente as que
     * calculam). O comportamento do regime vem da tabela
     * {@code REGIME_APURACAO_SIMPLES_NACIONAL}, resolvida na sequência.
     */
    private RegimeCalculoSimplesEnum resolverRegime(OperacaoSimplesInput operacao) {
        int crt = Integer.parseInt(operacao.CRT().trim());
        return switch (crt) {
            case 1 -> RegimeCalculoSimplesEnum.CRT_1;
            case 2 -> RegimeCalculoSimplesEnum.CRT_2;
            default -> RegimeCalculoSimplesEnum.CRT_3_TPREGIME_2;
        };
    }

    /**
     * Valida a data do fato gerador e a converte.
     *
     * @return a data convertida, ou {@code null} (com o erro VSN-012 acumulado)
     *         quando ausente ou inválida
     */
    private LocalDate validarData(String data, List<ErroDetalhe> erros, String baseURL) {
        if (data == null || data.isBlank()) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.DATA_INVALIDA, "data",
                    "A data do fato gerador é obrigatória.", baseURL));
            return null;
        }
        try {
            return LocalDate.parse(data.trim());
        } catch (DateTimeParseException ex) {
            erros.add(buildErroGlobal(CodigoValidacaoSimples.DATA_INVALIDA, "data",
                    String.format("Data inválida, recebido: '%s' (esperado YYYY-MM-DD).", data), baseURL));
            return null;
        }
    }

    /** Converte texto em inteiro de forma tolerante ({@code null} quando inválido). */
    private Integer parseInteiro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ErroDetalhe buildErroGlobal(CodigoValidacaoSimples codigo, String campo, String detail,
            String baseURL) {
        String pointer = "#/" + campo;
        String type = String.format(
                "%s/calculadora/observabilidade/simples-nacional/erros?codigo=%s", baseURL, codigo.getCode());
        return new ErroDetalhe(type, codigo.getTitulo(), codigo.getCode(), pointer, detail);
    }
}
