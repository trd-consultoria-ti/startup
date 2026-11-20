package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.aliquotasimples.model.LinhaAliquotasSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.MotivoResolucaoAliquota;
import br.gov.serpro.rtc.api.aliquotasimples.model.OrigemLinhaAliquotasEnum;
import br.gov.serpro.rtc.api.aliquotasimples.model.ParAliquotasSimples;
import br.gov.serpro.rtc.api.simplesnacional.collector.ItemCollectorSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.ErroGlobalSimples;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.model.input.ItemSimplesUnificadoInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObjetoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObservabilidadeSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.OperacaoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotaItemDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotasDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.TotalSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.repository.TipoDfeCalculoSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.AvaliacaoIssItem;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.CamposIssItem;
import br.gov.serpro.rtc.api.simplesnacional.service.EscolhaParAliquotasService.EscolhaParAliquotas;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.AliquotasItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ContextoItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.DadosAliquotasItem;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.EntradaItemSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ItemAProcessar;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ItemProcessadoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.ProcessadorItemSimples.ResolvedorAliquotasItem;
import br.gov.serpro.rtc.shared.exceptionhandler.ErrosCalculoException;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import lombok.RequiredArgsConstructor;

/**
 *
 * NÚCLEO dos endpoints não assistido e assistido do Simples Nacional, os mesmos
 * para todos os documentos fiscais. A única diferença entre os dois endpoints
 * é DE ONDE VEM A LINHA DE ALÍQUOTAS do contribuinte
 * ({@link FonteLinhaAliquotasSimples}): do corpo da requisição ou da Tabela de
 * Alíquotas Efetivas do Simples Nacional. Mandada no corpo, a mesma linha
 * produz o mesmo resultado.
 *
 * <p>
 * Passos:
 * <ol>
 * <li>validação global (HTTP 422): {@code id}, {@code versao}, {@code data},
 * {@code tpDoc} (habilitado na tabela de documentos, que dá a família:
 * vocabulário do regime, casas da alíquota, máximo de itens e Imposto
 * Seletivo), campos de regime no vocabulário do documento — domínios, campos
 * proibidos (VSN-033) e coerências C21a-10/20/30 (família CRT) e 313, E0162 e
 * E0166 (NFS-e) —, {@code tpEnteGov} e itens (VSN-011, VSN-034);</li>
 * <li>regime de apuração (código neutro) pela tabela de enquadramento;
 * combinação sem linha vigente ⇒ fora do escopo (VSN-019);</li>
 * <li>fator de transição de compra governamental (VSN-025);</li>
 * <li>linha de alíquotas — só quando o regime calcula tributos (no
 * {@code SOMENTE_RECEITA} a fonte não é consultada);</li>
 * <li>itens, pelo {@link ProcessadorItemSimples} compartilhado com o clássico,
 * com o par de alíquotas escolhido item a item pelo
 * {@link EscolhaParAliquotasService};</li>
 * <li>saída: {@code oper} com o código neutro, {@code resolucaoAliquotas}
 * (quando o regime calcula), {@code objetos} e {@code total}.</li>
 * </ol>
 *
 */
@RequiredArgsConstructor
@Service
public class CalculoSimplesUnificadoService {

    private static final String CAMPO_TPDOC = "tpDoc";
    private static final String CAMPO_CRT = "CRT";
    private static final String CAMPO_TPREGIMEIBSCBS = "tpRegimeIBSCBS";
    private static final String CAMPO_OPSIMPNAC = "opSimpNac";
    private static final String CAMPO_REGAPIBSCBSSN = "regApIBSCBSSN";
    private static final String CAMPO_REGAPTRIBSN = "regApTribSN";
    private static final String CAMPO_TPENTEGOV = "tpEnteGov";

    /** opSimpNac 3 (ME/EPP) e 4 (opção pendente): optantes, com regApIBSCBSSN e regApTribSN obrigatórios. */
    private static final Set<Integer> OPTANTES = Set.of(3, 4);

    private final ParametroCalculoSimplesService parametroService;
    private final ProcessadorItemSimples processadorItem;
    private final EscolhaParAliquotasService escolhaPar;
    private final TipoDfeCalculoSimplesRepository tipoDfeRepository;

    // ─── Contratos ─────────────────────────────────────────────────────────────

    /**
     * Dados da operação comuns aos endpoints não assistido e assistido (sem a
     * linha de alíquotas nem a identificação do contribuinte), como chegaram.
     */
    public record DadosOperacaoSimples(String id, String versao, String data, String tpDoc, String CRT,
            String tpRegimeIBSCBS, String opSimpNac, String regApIBSCBSSN, String regApTribSN, String tpEnteGov,
            List<ItemSimplesUnificadoInput> itens) {
    }

    /**
     * De onde vem a linha de alíquotas do contribuinte. Uma instância por
     * requisição.
     */
    public interface FonteLinhaAliquotasSimples {

        /**
         * Obtém a linha de alíquotas (pontos percentuais). Só é chamado quando o
         * regime calcula tributos.
         *
         * @throws ErrosCalculoException erros globais da fonte (linha ausente ou fora do formato)
         */
        LinhaAliquotasSimples obter(LocalDate data, String baseURL);

        /** Monta o bloco {@code resolucaoAliquotas} com o cabeçalho próprio da fonte. */
        ResolucaoAliquotasDomain montarResolucao(LinhaAliquotasSimples linha, List<ResolucaoAliquotaItemDomain> itens,
                List<String> avisos);
    }

    /** Campos de regime já convertidos (nulos quando ausentes). */
    private record RegimeInformado(Integer crt, Integer tpRegimeIBSCBS, Integer opSimpNac, Integer regApIBSCBSSN,
            Integer regApTribSN) {
    }

    // ─── Processamento ─────────────────────────────────────────────────────────

    public ObservabilidadeSimplesDomain processar(DadosOperacaoSimples operacao, FonteLinhaAliquotasSimples fonte,
            String baseURL) {
        // 1. Validação global (HTTP 422).
        List<ErroDetalhe> erros = new ArrayList<>();
        LocalDate data = validarCamposGlobais(operacao, erros, baseURL);
        TipoDfeCalculoSimples documento = validarDocumento(operacao, data, erros, baseURL);
        if (documento != null) {
            validarRegime(operacao, documento.getVocabularioRegime(), erros, baseURL);
            validarQuantidadeItens(operacao, documento, erros, baseURL);
        }
        if (!erros.isEmpty()) {
            throw new ErrosCalculoException(erros);
        }
        final VocabularioRegimeSimplesEnum vocabulario = documento.getVocabularioRegime();
        final String siglaDfe = tipoDfeRepository.buscarSiglaVigentePorId(documento.getTipoDfeId(), data)
                .orElse(null);
        final boolean permiteNcmZero = tipoDfeRepository.permiteNcmZeroVigentePorId(documento.getTipoDfeId(), data)
                .orElse(false);
        final RegimeInformado informado = converterRegime(operacao);

        // 2. Regime de apuração (código neutro) pela tabela de enquadramento.
        final RegimeApuracaoSimples regime = parametroService.buscarRegimeApuracao(vocabulario, informado.crt(),
                informado.tpRegimeIBSCBS(), informado.opSimpNac(), informado.regApIBSCBSSN(), data)
                .orElseThrow(() -> new ErrosCalculoException(List.of(erroForaDoEscopo(vocabulario, informado, data,
                        baseURL))));
        final boolean calculaTributos = Boolean.TRUE.equals(regime.getCalculaTributos());

        // 3. Fator de transição de compra governamental, como no clássico.
        final Integer tpEnteGov = EscolhaParAliquotasService.inteiro(operacao.tpEnteGov());
        BigDecimal fatorTransicao = null;
        if (tpEnteGov != null && tpEnteGov != 1) {
            fatorTransicao = parametroService.buscarFatorTransicaoCompraGov(data)
                    .orElseThrow(() -> new ErrosCalculoException(List.of(ErroGlobalSimples.de(
                            CodigoValidacaoSimples.COMPRA_GOV_SEM_VIGENCIA, CAMPO_TPENTEGOV,
                            String.format("Não há parametrização de transição de compra governamental vigente "
                                    + "para a data '%s'.", data),
                            baseURL))));
        }

        // 4. Linha de alíquotas: só quando o regime calcula tributos.
        final LinhaAliquotasSimples linha = calculaTributos ? fonte.obter(data, baseURL) : null;
        final List<SituacaoIssParAliquotaSimples> situacoes = calculaTributos
                ? parametroService.buscarSituacoesIssParAliquota(data)
                : List.of();
        List<String> avisos = avisosDaOperacao(vocabulario, regime, informado, linha);

        // 5. Itens.
        final ContextoItemSimples contexto = new ContextoItemSimples(data, Integer.parseInt(operacao.tpDoc().trim()),
                regime, tpEnteGov, fatorTransicao, vocabulario, true,
                Boolean.TRUE.equals(documento.getAdmiteImpostoSeletivo()), documento.getCasasAliquotaCalculo(),
                siglaDfe, permiteNcmZero);
        final Map<String, Integer> fatosDaOperacao = fatosDaOperacao(informado);
        List<ItemAProcessar> itens = new ArrayList<>();
        for (int i = 0; i < operacao.itens().size(); i++) {
            ItemSimplesUnificadoInput item = itemSeguro(operacao.itens().get(i));
            itens.add(new ItemAProcessar(
                    new EntradaItemSimples(item.numero(), item.cAtivSN(), item.cClassTrib(), item.baseCalculo(),
                            item.valorImpostoSeletivo(), item.NCM(), item.NBS()),
                    calculaTributos ? resolvedor(item, vocabulario, linha, fatosDaOperacao, situacoes)
                            : SEM_ALIQUOTAS));
        }
        List<ItemProcessadoSimples> processados = processadorItem.processarItens(itens, contexto, baseURL);

        List<ObjetoSimplesDomain> objetos = processados.stream()
                .map(ItemProcessadoSimples::objeto)
                .sorted(Comparator.comparing(ObjetoSimplesDomain::nObj,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        ResolucaoAliquotasDomain resolucao = calculaTributos
                ? fonte.montarResolucao(linha, processados.stream().map(ItemProcessadoSimples::resolucao).toList(),
                        avisos)
                : null;

        // 6. Saída.
        return new ObservabilidadeSimplesDomain(
                new OperacaoSimplesDomain(operacao.data(), operacao.tpDoc(), operacao.CRT(), operacao.tpRegimeIBSCBS(),
                        operacao.opSimpNac(), operacao.regApIBSCBSSN(), operacao.regApTribSN(), operacao.tpEnteGov(),
                        regime.getCodigo()),
                resolucao,
                objetos,
                TotalSimplesDomain.totalizar(objetos));
    }

    // ─── Alíquotas do item: o par escolhido na linha do contribuinte ───────────

    /** Regime que não calcula tributos: nenhuma alíquota (o processador nem chama o resolvedor). */
    private static final ResolvedorAliquotasItem SEM_ALIQUOTAS =
            (dados, collector) -> new AliquotasItemSimples(null, null, false, null);

    /**
     * Resolvedor do item: avalia os campos do ISS, escolhe o par na linha de
     * alíquotas e confere a exigência das alíquotas:
     * a CBS do par quando o tpRBSN exige alíquota; o IBS quando, além disso, o
     * regime gera IBS. Ausente na linha da requisição ⇒ VSN-004/VSN-006, com o
     * ponteiro para dentro de {@code aliquotasEfetivas}; CBS nula na tabela do
     * contribuinte ⇒ dado inconsistente, item {@code NAO_CALCULADO}.
     */
    private ResolvedorAliquotasItem resolvedor(ItemSimplesUnificadoInput item, VocabularioRegimeSimplesEnum vocabulario,
            LinhaAliquotasSimples linha, Map<String, Integer> fatosDaOperacao,
            List<SituacaoIssParAliquotaSimples> situacoes) {
        return new ResolvedorAliquotasItem() {

            @Override
            public AliquotasItemSimples resolver(DadosAliquotasItem dados, ItemCollectorSimples collector) {
                String cAtivSN = dados.atividade().cAtivSN();
                if (dados.atividade().atividade() == null) {
                    return semPar(cAtivSN, MotivoResolucaoAliquota.ATIVIDADE_DESCONHECIDA, List.of());
                }

                List<String> avisos = new ArrayList<>();
                Map<String, Integer> fatos = new HashMap<>(fatosDaOperacao);
                if (dados.atividade().admissivel()) {
                    AvaliacaoIssItem iss = escolhaPar.avaliarCamposIss(vocabulario, dados.atividade().atividade(),
                            new CamposIssItem(item.tribISSQN(), item.tpRetISSQN(), item.tpBM()), collector);
                    avisos.addAll(iss.avisos());
                    colocar(fatos, EscolhaParAliquotasService.CAMPO_TRIBISSQN, iss.tribISSQN());
                    colocar(fatos, EscolhaParAliquotasService.CAMPO_TPRETISSQN, iss.tpRetISSQN());
                    colocar(fatos, EscolhaParAliquotasService.CAMPO_TPBM, iss.tpBM());
                }

                EscolhaParAliquotas escolha = escolhaPar.escolher(dados.atividade().atividade(),
                        dados.atividade().admissivel(), linha, fatos, situacoes);
                if (escolha.motivo() == MotivoResolucaoAliquota.FATOR_R_AUSENTE) {
                    collector.addErroValidacaoNoPonteiro(CodigoValidacaoSimples.FATOR_R_AUSENTE,
                            ponteiroLinha(collector, "fatorR"), String.format("O fatorR é exigido: a atividade %s "
                                    + "é sujeita ao fator \"r\" (Anexo III ou V).", cAtivSN));
                }
                if (escolha.parSemRedistribuicaoAusente()) {
                    avisos.add(EscolhaParAliquotasService.avisoParSemRedistribuicaoAusente(escolha.anexo()));
                }

                ParAliquotasSimples par = escolha.par();
                BigDecimal pCbs = par == null ? null : par.pCBSSN();
                BigDecimal pIbs = par == null ? null : par.pIBSSN();
                boolean naoCalcular = false;
                if (escolha.caminhoPar() != null) {
                    if (linha.origem() == OrigemLinhaAliquotasEnum.TABELA_ALIQUOTAS_EFETIVAS && pCbs == null) {
                        // Dado inconsistente na Tabela de Alíquotas Efetivas: o item não é calculado.
                        naoCalcular = true;
                    } else {
                        exigir(dados.cbsExigida(), pCbs, CodigoValidacaoSimples.ALIQUOTA_CBS_AUSENTE, "pCBSSN",
                                escolha, collector);
                        exigir(dados.ibsExigida(), pIbs, CodigoValidacaoSimples.ALIQUOTA_IBS_AUSENTE, "pIBSSN",
                                escolha, collector);
                    }
                }

                return new AliquotasItemSimples(pIbs, pCbs, naoCalcular, new ResolucaoAliquotaItemDomain(
                        item.numero(), cAtivSN, escolha.anexo(), escolha.tipoAliquota(), escolha.regra(), pIbs, pCbs,
                        escolha.motivo(), avisos));
            }

            @Override
            public ResolucaoAliquotaItemDomain naoAvaliado() {
                return semPar(ProcessadorItemSimples.normalizarCAtivSN(item.cAtivSN(), true),
                        MotivoResolucaoAliquota.ITEM_NAO_IMPLEMENTADO, List.of()).resolucao();
            }

            private AliquotasItemSimples semPar(String cAtivSN, MotivoResolucaoAliquota motivo, List<String> avisos) {
                return new AliquotasItemSimples(null, null, false, new ResolucaoAliquotaItemDomain(item.numero(),
                        cAtivSN, null, null, null, null, null, motivo, avisos));
            }

            /** Alíquota exigida e ausente no par escolhido ⇒ erro do item, com o caminho do par. */
            private void exigir(boolean exigida, BigDecimal aliquota, CodigoValidacaoSimples codigo, String campo,
                    EscolhaParAliquotas escolha, ItemCollectorSimples collector) {
                if (!exigida || aliquota != null) {
                    return;
                }
                String par = escolha.tipoAliquota() == TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO
                        && !escolha.parSemRedistribuicaoAusente() ? "sem" : "com";
                String detalhe = escolha.tipoAliquota() == TipoAliquotaSimplesEnum.PAR_UNICO
                        ? String.format("A alíquota %s do anexo %s é exigida para o cálculo do item e não está na "
                                + "linha de alíquotas.", campo, escolha.anexo())
                        : String.format("A alíquota %s do anexo %s (par %s redistribuição do ISS, regra %s) é "
                                + "exigida para o cálculo do item e não está na linha de alíquotas.", campo,
                                escolha.anexo(), par, escolha.regra());
                collector.addErroValidacaoNoPonteiro(codigo, ponteiroLinha(collector, escolha.caminhoPar() + "/"
                        + campo), detalhe);
            }

            /**
             * Ponteiro do valor da linha: dentro de {@code aliquotasEfetivas} quando a
             * linha veio da requisição; no {@code cAtivSN} do item quando veio da
             * Tabela de Alíquotas Efetivas (que não está no corpo).
             */
            private String ponteiroLinha(ItemCollectorSimples collector, String caminho) {
                return linha.origem() == OrigemLinhaAliquotasEnum.REQUISICAO
                        ? "#/aliquotasEfetivas/" + caminho
                        : "#/itens/" + collector.getIndex() + "/cAtivSN";
            }
        };
    }

    private static void colocar(Map<String, Integer> fatos, String campo, Integer valor) {
        if (valor != null) {
            fatos.put(campo, valor);
        }
    }

    /** Fatos da operação que entram na escolha do par (regra 4), pelo nome do contrato. */
    private static Map<String, Integer> fatosDaOperacao(RegimeInformado informado) {
        Map<String, Integer> fatos = new HashMap<>();
        colocar(fatos, EscolhaParAliquotasService.CAMPO_CRT, informado.crt());
        colocar(fatos, EscolhaParAliquotasService.CAMPO_REGAPIBSCBSSN, informado.regApIBSCBSSN());
        colocar(fatos, EscolhaParAliquotasService.CAMPO_REGAPTRIBSN, informado.regApTribSN());
        return Map.copyOf(fatos);
    }

    /** Item nulo na lista de entrada vira item vazio (os erros obrigatórios aparecem no próprio item). */
    private static ItemSimplesUnificadoInput itemSeguro(ItemSimplesUnificadoInput item) {
        return item != null ? item
                : new ItemSimplesUnificadoInput(null, null, null, null, null, null, null, null, null, null);
    }

    // ─── Avisos da operação ────────────────────────────────────────────────────

    /**
     * Avisos que não bloqueiam: divergência entre o
     * regime e o indicador de impedimento por sublimite da linha de alíquotas
     * (regime que gera IBS com impedimento; regime que só calcula a CBS sem
     * impedimento) e, na NFS-e, {@code regApIBSCBSSN = 2} com
     * {@code regApTribSN = 1}.
     */
    private static List<String> avisosDaOperacao(VocabularioRegimeSimplesEnum vocabulario, RegimeApuracaoSimples regime,
            RegimeInformado informado, LinhaAliquotasSimples linha) {
        List<String> avisos = new ArrayList<>();
        if (linha != null) {
            boolean geraIbs = Boolean.TRUE.equals(regime.getGeraIbs());
            if (geraIbs == linha.impedimentoSublimite()) {
                avisos.add(String.format("Regime de apuração %s (%s) diverge do indicador de impedimento por "
                        + "sublimite da linha de alíquotas (%s)", regime.getCodigo(),
                        descreverRegime(vocabulario, informado), linha.impedimentoSublimite()));
            }
        }
        if (Integer.valueOf(2).equals(informado.regApIBSCBSSN()) && Integer.valueOf(1).equals(informado.regApTribSN())) {
            avisos.add("regApIBSCBSSN 2 (excesso de sublimite) com regApTribSN 1: incoerente — quem excede o "
                    + "sublimite tem o ISS fora do Simples Nacional");
        }
        return avisos;
    }

    private static String descreverRegime(VocabularioRegimeSimplesEnum vocabulario, RegimeInformado informado) {
        if (vocabulario == VocabularioRegimeSimplesEnum.NFSE) {
            return String.format("opSimpNac=%d, regApIBSCBSSN=%d", informado.opSimpNac(), informado.regApIBSCBSSN());
        }
        return informado.tpRegimeIBSCBS() == null ? String.format("CRT=%d", informado.crt())
                : String.format("CRT=%d, tpRegimeIBSCBS=%d", informado.crt(), informado.tpRegimeIBSCBS());
    }

    // ─── Validação global ──────────────────────────────────────────────────────

    /** id, versao, data, tpEnteGov e itens; devolve a data convertida (ou {@code null}, com VSN-012). */
    private static LocalDate validarCamposGlobais(DadosOperacaoSimples operacao, List<ErroDetalhe> erros,
            String baseURL) {
        if (operacao.id() == null || operacao.id().isBlank()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.DADO_GLOBAL_AUSENTE, "id",
                    "O identificador da operação (id) é obrigatório.", baseURL));
        }
        if (operacao.versao() == null || operacao.versao().isBlank()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.DADO_GLOBAL_AUSENTE, "versao",
                    "A versão do payload (versao) é obrigatória.", baseURL));
        }
        LocalDate data = null;
        if (operacao.data() == null || operacao.data().isBlank()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.DATA_INVALIDA, "data",
                    "A data do fato gerador é obrigatória.", baseURL));
        } else {
            try {
                data = LocalDate.parse(operacao.data().trim());
            } catch (DateTimeParseException ex) {
                erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.DATA_INVALIDA, "data",
                        String.format("Data inválida, recebido: '%s' (esperado YYYY-MM-DD).", operacao.data()),
                        baseURL));
            }
        }
        if (operacao.tpEnteGov() != null && !operacao.tpEnteGov().isBlank()) {
            Integer tpEnteGov = EscolhaParAliquotasService.inteiro(operacao.tpEnteGov());
            if (tpEnteGov == null || tpEnteGov < 1 || tpEnteGov > 4) {
                erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPENTEGOV_INVALIDO, CAMPO_TPENTEGOV,
                        String.format("Tipo de ente governamental inválido, recebido: '%s' (esperado 1, 2, 3 ou 4).",
                                operacao.tpEnteGov()), baseURL));
            }
        }
        if (operacao.itens() == null || operacao.itens().isEmpty()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.ITENS_VAZIOS, "itens",
                    "A lista de itens é obrigatória e não pode estar vazia.", baseURL));
        }
        return data;
    }

    /**
     * tpDoc obrigatório e habilitado para o cálculo na data (VSN-014/015);
     * documento conhecido sem tratamento na Calculadora (coluna da tabela de
     * tipos de documento, vigente na data) também é VSN-015. A
     * linha da tabela de documentos dá a família do documento; sem ela (tpDoc
     * ou data inválidos), os campos de regime não são validados.
     */
    private TipoDfeCalculoSimples validarDocumento(DadosOperacaoSimples operacao, LocalDate data,
            List<ErroDetalhe> erros, String baseURL) {
        if (operacao.tpDoc() == null || operacao.tpDoc().isBlank()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPDOC_AUSENTE, CAMPO_TPDOC,
                    "O tipo de documento fiscal (tpDoc) é obrigatório e não foi informado.", baseURL));
            return null;
        }
        Integer tpDoc = EscolhaParAliquotasService.inteiro(operacao.tpDoc());
        if (tpDoc != null && data != null && parametroService.tpDocSemTratamentoCalculadora(tpDoc, data)) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, CAMPO_TPDOC,
                    String.format("Tipo de documento fiscal (tpDoc) de código %d ainda não possui tratamento "
                            + "implementado no cálculo do Simples Nacional.", tpDoc), baseURL));
            return null;
        }
        if (tpDoc == null || data == null) {
            if (tpDoc == null) {
                erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, CAMPO_TPDOC,
                        String.format("Tipo de documento não suportado pelo cálculo do Simples Nacional, recebido: "
                                + "'%s'.", operacao.tpDoc()), baseURL));
            }
            return null;
        }
        return parametroService.buscarTipoDfeCalculo(tpDoc, data).orElseGet(() -> {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPDOC_NAO_SUPORTADO, CAMPO_TPDOC,
                    String.format("Tipo de documento não suportado pelo cálculo do Simples Nacional, recebido: '%s'.",
                            operacao.tpDoc()), baseURL));
            return null;
        });
    }

    /** Número de itens acima do máximo do documento (NFS-e: 1) ⇒ VSN-034. */
    private static void validarQuantidadeItens(DadosOperacaoSimples operacao, TipoDfeCalculoSimples documento,
            List<ErroDetalhe> erros, String baseURL) {
        Integer maximo = documento.getMaximoItens();
        if (maximo != null && operacao.itens() != null && operacao.itens().size() > maximo) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.QUANTIDADE_ITENS_INVALIDA, "itens",
                    String.format("O documento %s admite %s, recebidos %d.", operacao.tpDoc().trim(),
                            maximo == 1 ? "exatamente 1 item" : "no máximo " + maximo + " itens",
                            operacao.itens().size()), baseURL));
        }
    }

    /** Validação estrutural dos campos de regime, no vocabulário do documento. */
    private static void validarRegime(DadosOperacaoSimples operacao, VocabularioRegimeSimplesEnum vocabulario,
            List<ErroDetalhe> erros, String baseURL) {
        if (vocabulario == VocabularioRegimeSimplesEnum.NFSE) {
            proibido(operacao.CRT(), CAMPO_CRT, "NFS-e", erros, baseURL);
            proibido(operacao.tpRegimeIBSCBS(), CAMPO_TPREGIMEIBSCBS, "NFS-e", erros, baseURL);
            validarRegimeNfse(operacao, erros, baseURL);
        } else {
            proibido(operacao.opSimpNac(), CAMPO_OPSIMPNAC, "documento com CRT", erros, baseURL);
            proibido(operacao.regApIBSCBSSN(), CAMPO_REGAPIBSCBSSN, "documento com CRT", erros, baseURL);
            proibido(operacao.regApTribSN(), CAMPO_REGAPTRIBSN, "documento com CRT", erros, baseURL);
            validarRegimeCrt(operacao, erros, baseURL);
        }
    }

    private static void proibido(String valor, String campo, String documento, List<ErroDetalhe> erros,
            String baseURL) {
        if (valor != null && !valor.isBlank()) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.CAMPO_REGIME_PROIBIDO, campo,
                    String.format("O campo %s não pertence ao vocabulário de regime do %s, recebido: '%s'.", campo,
                            documento, valor), baseURL));
        }
    }

    /** Família CRT: CRT 1 a 4 (VSN-016), tpRegimeIBSCBS 1 ou 2 (VSN-017) e coerências C21a-10/20/30 (VSN-018). */
    private static void validarRegimeCrt(DadosOperacaoSimples operacao, List<ErroDetalhe> erros, String baseURL) {
        Integer crt = EscolhaParAliquotasService.inteiro(operacao.CRT());
        if (crt == null || crt < 1 || crt > 4) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.CRT_INVALIDO, CAMPO_CRT,
                    String.format("O Código de Regime Tributário (CRT) é obrigatório e deve ser 1, 2, 3 ou 4, "
                            + "recebido: '%s'.", operacao.CRT()), baseURL));
            return;
        }
        boolean tpRegimeInformado = operacao.tpRegimeIBSCBS() != null && !operacao.tpRegimeIBSCBS().isBlank();
        if (!tpRegimeInformado) {
            return;
        }
        Integer tpRegime = EscolhaParAliquotasService.inteiro(operacao.tpRegimeIBSCBS());
        if (tpRegime == null || (tpRegime != 1 && tpRegime != 2)) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPREGIMEIBSCBS_INVALIDO, CAMPO_TPREGIMEIBSCBS,
                    String.format("Tipo de regime de apuração do IBS/CBS inválido, recebido: '%s' (esperado 1 ou 2).",
                            operacao.tpRegimeIBSCBS()), baseURL));
            return;
        }
        if (crt == 4) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    tpRegime == 2
                            ? "CRT=4 (MEI) não admite tpRegimeIBSCBS algum (regras C21a-10 e C21a-30 da NT 2026.005)."
                            : "CRT=4 (MEI) não admite tpRegimeIBSCBS algum (regra C21a-30 da NT 2026.005).",
                    baseURL));
        } else if ((crt == 1 || crt == 2) && tpRegime == 2) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    String.format("CRT=%d não admite tpRegimeIBSCBS=2 (regra C21a-10 da NT 2026.005).", crt),
                    baseURL));
        } else if (crt == 3 && tpRegime == 1) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.TPREGIMEIBSCBS_INCOMPATIVEL, CAMPO_TPREGIMEIBSCBS,
                    "CRT=3 não admite tpRegimeIBSCBS=1 (regra C21a-20 da NT 2026.005).", baseURL));
        }
    }

    /**
     * Família NFS-e: opSimpNac 1 a 4 (VSN-030); regApIBSCBSSN (VSN-031) e
     * regApTribSN (VSN-032) de 1 a 3, obrigatórios com opSimpNac 3 ou 4 (regra
     * 313; regra E0166, estendida pela NT 009) e proibidos com 1 ou 2 (E0162).
     */
    private static void validarRegimeNfse(DadosOperacaoSimples operacao, List<ErroDetalhe> erros, String baseURL) {
        Integer opSimpNac = EscolhaParAliquotasService.inteiro(operacao.opSimpNac());
        if (opSimpNac == null || opSimpNac < 1 || opSimpNac > 4) {
            erros.add(ErroGlobalSimples.de(CodigoValidacaoSimples.OPSIMPNAC_INVALIDO, CAMPO_OPSIMPNAC,
                    String.format("A situação perante o Simples Nacional (opSimpNac) é obrigatória na NFS-e e deve ser "
                            + "1, 2, 3 ou 4, recebido: '%s'.", operacao.opSimpNac()), baseURL));
            return;
        }
        validarCampoNfse(operacao.regApIBSCBSSN(), CAMPO_REGAPIBSCBSSN, opSimpNac,
                CodigoValidacaoSimples.REGAPIBSCBSSN_INVALIDO, "regra 313 da NFS-e", erros, baseURL);
        validarCampoNfse(operacao.regApTribSN(), CAMPO_REGAPTRIBSN, opSimpNac,
                CodigoValidacaoSimples.REGAPTRIBSN_INVALIDO, "regra E0166 da NFS-e, estendida pela NT 009", erros,
                baseURL);
    }

    private static void validarCampoNfse(String valor, String campo, int opSimpNac, CodigoValidacaoSimples codigo,
            String regraObrigatorio, List<ErroDetalhe> erros, String baseURL) {
        boolean informado = valor != null && !valor.isBlank();
        if (informado) {
            Integer convertido = EscolhaParAliquotasService.inteiro(valor);
            if (convertido == null || convertido < 1 || convertido > 3) {
                erros.add(ErroGlobalSimples.de(codigo, campo,
                        String.format("O %s deve ser 1, 2 ou 3, recebido: '%s'.", campo, valor), baseURL));
            } else if (!OPTANTES.contains(opSimpNac)) {
                erros.add(ErroGlobalSimples.de(codigo, campo, String.format("O %s não pode ser informado com "
                        + "opSimpNac=%d (não optante ou MEI)%s.", campo, opSimpNac,
                        CAMPO_REGAPTRIBSN.equals(campo) ? " (regra E0162 da NFS-e)" : ""), baseURL));
            }
        } else if (OPTANTES.contains(opSimpNac)) {
            erros.add(ErroGlobalSimples.de(codigo, campo, String.format("O %s é obrigatório com opSimpNac=%d (%s).",
                    campo, opSimpNac, regraObrigatorio), baseURL));
        }
    }

    private static RegimeInformado converterRegime(DadosOperacaoSimples operacao) {
        return new RegimeInformado(EscolhaParAliquotasService.inteiro(operacao.CRT()),
                EscolhaParAliquotasService.inteiro(operacao.tpRegimeIBSCBS()),
                EscolhaParAliquotasService.inteiro(operacao.opSimpNac()),
                EscolhaParAliquotasService.inteiro(operacao.regApIBSCBSSN()),
                EscolhaParAliquotasService.inteiro(operacao.regApTribSN()));
    }

    /**
     * Combinação válida no leiaute sem linha vigente na tabela de enquadramento:
     * fora do escopo do cálculo (VSN-019), com a mensagem no vocabulário recebido.
     */
    private static ErroDetalhe erroForaDoEscopo(VocabularioRegimeSimplesEnum vocabulario, RegimeInformado informado,
            LocalDate data, String baseURL) {
        String motivo;
        String campo;
        if (vocabulario == VocabularioRegimeSimplesEnum.NFSE) {
            campo = CAMPO_OPSIMPNAC;
            motivo = switch (informado.opSimpNac()) {
                case 1 -> "prestador não optante pelo Simples Nacional (opSimpNac=1)";
                case 2 -> "MEI (opSimpNac=2) está fora do cálculo do IBS e da CBS pelo Simples Nacional";
                default -> String.format("não há enquadramento vigente na data '%s' para opSimpNac=%d e "
                        + "regApIBSCBSSN=%d", data, informado.opSimpNac(), informado.regApIBSCBSSN());
            };
        } else {
            campo = CAMPO_CRT;
            if (informado.crt() == 4) {
                motivo = "MEI (CRT=4) está fora do Evento de Tributação do Simples Nacional";
            } else if (informado.crt() == 3 && informado.tpRegimeIBSCBS() == null) {
                motivo = "CRT=3 sem tpRegimeIBSCBS=2 (regime normal puro) não é Simples Nacional";
            } else {
                motivo = String.format("não há enquadramento vigente na data '%s' para %s", data,
                        descreverRegime(vocabulario, informado));
            }
        }
        return ErroGlobalSimples.de(CodigoValidacaoSimples.OPERACAO_FORA_ESCOPO, campo,
                "Operação fora do escopo do cálculo do Simples Nacional: " + motivo + ".", baseURL);
    }
}
