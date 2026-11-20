package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.simplesnacional.adapter.ValidacaoNomenclaturaAdapter;
import br.gov.serpro.rtc.api.simplesnacional.collector.ItemCollectorSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.MotivoNaoCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.model.output.MotivoEstadoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObjetoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotaItemDomain;
import br.gov.serpro.rtc.api.simplesnacional.repository.AtividadeSimplesCalculoRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.InsumoCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.SimplesNacionalCalculoService.ResultadoCalculoSimples;
import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Processamento POR ITEM do cálculo do Simples Nacional, compartilhado pelo
 * endpoint clássico e pelos dois endpoints novos (não assistido e
 * assistido).
 *
 * <p>
 * Extraído do {@link SimplesNacionalObservabilidadeService}, com a mesma
 * sequência de sempre:
 * <ol>
 * <li>verificação PRIORITÁRIA de cClassTrib ainda não implementado (VSN-027,
 * estado {@code NAO_IMPLEMENTADO}) — primeira consulta de banco do item;</li>
 * <li>validação da base de cálculo e do Imposto Seletivo;</li>
 * <li>resolução do tipo de receita bruta pelo cClassTrib;</li>
 * <li>validação do código de atividade (cAtivSN) — inválido leva a
 * {@code NAO_CALCULADO}, sem erro, com o motivo
 * ({@link MotivoNaoCalculoSimplesEnum});</li>
 * <li>alíquotas, pela {@link ResolvedorAliquotasItem} de cada endpoint: no
 * clássico, os campos {@code pIBSSN}/{@code pCBSSN} do item; nos endpoints
 * novos, o par escolhido na linha de alíquotas do contribuinte;</li>
 * <li>cálculo puro ({@link SimplesNacionalCalculoService}).</li>
 * </ol>
 *
 * <p>
 * O que muda por endpoint vem do {@link ContextoItemSimples}: o clássico usa
 * sempre a linha padrão do tpRBSN, não normaliza o cAtivSN, exige o Imposto
 * Seletivo e calcula com a precisão integral — exatamente como antes. Os
 * endpoints novos aplicam o que a tabela de documentos parametriza para o
 * tpDoc (vocabulário, Imposto Seletivo e casas da alíquota) e o regime
 * {@code SOMENTE_RECEITA}, que não calcula tributos.
 *
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProcessadorItemSimples {

    /** Nome de campo do contrato usado em JSON Pointers e mensagens. */
    private static final String CAMPO_CCLASSTRIB = "cClassTrib";

    /** Um único dígito: completado com zero à esquerda ("7" ⇒ "07"), porque na DPS o campo é numérico. */
    private static final Pattern UM_DIGITO = Pattern.compile("\\d");

    /**
     * Formato aceito para alíquotas (pontos percentuais): até 3 dígitos inteiros e até 34 casas
     * decimais, sem sinal, sem expoente. Validado ANTES de {@code new BigDecimal}: notação
     * científica (ex.: {@code 1E+100000000}) esgotaria a memória no cálculo.
     */
    public static final Pattern FORMATO_ALIQUOTA = Pattern.compile(String.format("\\d{1,%d}(\\.\\d{1,%d})?",
            ArredondamentoUtils.DIGITOS_INTEIROS_ALIQUOTA_ENTRADA, ArredondamentoUtils.CASAS_ALIQUOTA_ENTRADA));

    /** Formato aceito para valores monetários: tipo 13v2 (até 13 dígitos inteiros e até 2 casas). */
    private static final Pattern FORMATO_VALOR = Pattern.compile(String.format("\\d{1,%d}(\\.\\d{1,%d})?",
            ArredondamentoUtils.DIGITOS_INTEIROS_VALOR_ENTRADA, ArredondamentoUtils.ESCALA_VALOR_FISCAL));

    public static final String DESCRICAO_FORMATO_ALIQUOTA = String.format(
            "até %d dígitos inteiros e até %d casas decimais, sem notação científica (ex.: 1.6839)",
            ArredondamentoUtils.DIGITOS_INTEIROS_ALIQUOTA_ENTRADA, ArredondamentoUtils.CASAS_ALIQUOTA_ENTRADA);

    private static final String DESCRICAO_FORMATO_VALOR = String.format(
            "até %d dígitos inteiros e até %d casas decimais, sem notação científica (ex.: 1000.00)",
            ArredondamentoUtils.DIGITOS_INTEIROS_VALOR_ENTRADA, ArredondamentoUtils.ESCALA_VALOR_FISCAL);

    private final SimplesNacionalCalculoService calculoService;
    private final ParametroCalculoSimplesService parametroService;
    private final ClassificacaoTributariaSimplesRepository classificacaoTributariaRepository;
    private final AtividadeSimplesCalculoRepository atividadeCalculoRepository;
    private final ValidacaoNomenclaturaAdapter validacaoNomenclaturaSimples;

    // ─── Contratos ─────────────────────────────────────────────────────────────

    /** Os campos do item que o processamento comum usa, qualquer que seja o contrato de entrada. */
    public record EntradaItemSimples(Integer numero, String cAtivSN, String cClassTrib, String baseCalculo,
            String valorImpostoSeletivo, String NCM, String NBS) {
    }

    /**
     * Dados da operação já validados que valem para todos os itens.
     *
     * @param data                     data do fato gerador (rege as vigências)
     * @param tpDoc                    tipo do documento fiscal
     * @param regime                   linha vigente da matriz de regime
     * @param tpEnteGov                tipo de ente governamental (compra governamental), ou {@code null}
     * @param fatorTransicaoCompraGov  fator de transição já resolvido, ou {@code null}
     * @param vocabularioTipoReceita   vocabulário das linhas do tpRBSN; {@code null} = só a linha padrão
     *                                 (endpoint clássico)
     * @param normalizarCAtivSN        completa com zero à esquerda o cAtivSN de um dígito (endpoints novos)
     * @param admiteImpostoSeletivo    {@code false}: o Imposto Seletivo é opcional e tem de ser zero
     *                                 (VSN-035) e não sai na resposta (NFS-e)
     * @param casasAliquotaCalculo     casas da alíquota no cálculo; {@code null} = precisão integral
     */
    public record ContextoItemSimples(LocalDate data, int tpDoc, RegimeApuracaoSimples regime, Integer tpEnteGov,
            BigDecimal fatorTransicaoCompraGov, VocabularioRegimeSimplesEnum vocabularioTipoReceita,
            boolean normalizarCAtivSN, boolean admiteImpostoSeletivo, Integer casasAliquotaCalculo, String siglaDfe,
            boolean permiteNcmZero) {

        /** Contexto do endpoint clássico: comportamento de sempre, qualquer que seja o tpDoc. */
        public static ContextoItemSimples classico(LocalDate data, int tpDoc, RegimeApuracaoSimples regime,
                Integer tpEnteGov, BigDecimal fatorTransicaoCompraGov, String siglaDfe, boolean permiteNcmZero) {
            return new ContextoItemSimples(data, tpDoc, regime, tpEnteGov, fatorTransicaoCompraGov, null, false, true,
                    null, siglaDfe, permiteNcmZero);
        }
    }

    /** Classificação CBS/IBS já resolvida e seu efeito no Simples, evitando novo lookup no item. */
    public record ResolucaoClassificacaoItem(ClassificacaoTributariaSimples classificacao,
            TipoReceitaBrutaSimples tipoReceita) {}

    /**
     * Atividade do item: a linha vigente do catálogo ({@code null} quando o
     * cAtivSN está ausente, é desconhecido ou está fora de vigência) e se ela é
     * admitida no tpDoc da operação.
     */
    public record AtividadeItemSimples(String cAtivSN, AtividadeSimplesNacional atividade, boolean admissivel) {

        /** O cAtivSN serve para o cálculo: vigente e admitido no documento. */
        public boolean valida() {
            return atividade != null && admissivel;
        }
    }

    /**
     * O que o resolvedor de alíquotas recebe: o índice do item, a atividade, o
     * tipo de receita bruta e a exigência das alíquotas — a CBS quando o
     * cAtivSN é válido e o tpRBSN exige alíquota; o IBS quando, além disso, o
     * regime gera IBS.
     */
    public record DadosAliquotasItem(int index, AtividadeItemSimples atividade, TipoReceitaBrutaSimples tipoReceita,
            boolean cbsExigida, boolean ibsExigida) {
    }

    /**
     * Alíquotas a usar no cálculo do item (pontos percentuais, precisão integral, ou
     * nulas), mais o que o endpoint precisa mostrar em {@code resolucaoAliquotas}.
     *
     * @param naoCalcular {@code true}: o item sai {@code NAO_CALCULADO} mesmo com cAtivSN válido
     *                    (assistido sem a CBS do par escolhido: dado inconsistente)
     * @param resolucao   resolução do item para o bloco {@code resolucaoAliquotas}; {@code null} no clássico
     */
    public record AliquotasItemSimples(BigDecimal pIBSSN, BigDecimal pCBSSN, boolean naoCalcular,
            ResolucaoAliquotaItemDomain resolucao) {
    }

    /** De onde vêm as alíquotas do item: cada endpoint tem a sua. */
    public interface ResolvedorAliquotasItem {

        /**
         * Resolve as alíquotas do item, acumulando no coletor os erros de
         * entrada que encontrar.
         */
        AliquotasItemSimples resolver(DadosAliquotasItem dados, ItemCollectorSimples collector);

        /** Resolução mostrada quando o item é {@code NAO_IMPLEMENTADO} e não chega às alíquotas. */
        default ResolucaoAliquotaItemDomain naoAvaliado() {
            return null;
        }
    }

    /** Resultado do item: o objeto da resposta e a sua resolução de alíquotas (nula no clássico). */
    public record ItemProcessadoSimples(ObjetoSimplesDomain objeto, ResolucaoAliquotaItemDomain resolucao) {
    }

    /** Um item a processar: os campos comuns e o resolvedor de alíquotas dele. */
    public record ItemAProcessar(EntradaItemSimples entrada, ResolvedorAliquotasItem resolvedor) {
    }

    // ─── Processamento ─────────────────────────────────────────────────────────

    /**
     * Processa os itens em paralelo com virtual threads e devolve os resultados
     * na ORDEM DE ENTRADA (a ordenação da resposta é de quem chama).
     */
    public List<ItemProcessadoSimples> processarItens(List<ItemAProcessar> itens, ContextoItemSimples contexto,
            String baseURL) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ItemProcessadoSimples>> futures = new ArrayList<>();
            for (int i = 0; i < itens.size(); i++) {
                final int index = i;
                final ItemAProcessar item = itens.get(index);
                futures.add(CompletableFuture.supplyAsync(
                        () -> processarItem(item.entrada(), index, contexto, item.resolvedor(), baseURL), executor));
            }
            return futures.stream().map(CompletableFuture::join).toList();
        }
    }

    /** Processa um item; erros de validação ficam no próprio item (RFC 7807), nunca na operação. */
    public ItemProcessadoSimples processarItem(EntradaItemSimples item, int index, ContextoItemSimples contexto,
            ResolvedorAliquotasItem resolvedor, String baseURL) {
        ItemCollectorSimples collector = new ItemCollectorSimples(index, baseURL);

        Integer nObj = item.numero();
        if (nObj == null) {
            collector.addErroValidacao(CodigoValidacaoSimples.NUMERO_AUSENTE, "numero",
                    "O número do item é obrigatório.");
        }

        // 0. PRIORIDADE MÁXIMA — primeira consulta de banco do item: cClassTrib
        // vinculado, na data do fato gerador, a tratamento tributário marcado
        // como "sem implementação" (indicador de ajuste, TRTR_IN_POSSUI_AJUSTE).
        // Encerra o processamento do item: nada mais é validado nem calculado;
        // o item sai NAO_IMPLEMENTADO (VSN-027), sem tribCalc e fora dos totais.
        if (cClassTribNaoImplementado(item.cClassTrib(), contexto, collector)) {
            return new ItemProcessadoSimples(new ObjetoSimplesDomain(nObj != null ? nObj : index + 1,
                    collector.getEstado(), null, collector.getErros()), resolvedor.naoAvaliado());
        }

        // A regra do NCM zero depende do DF-e real selecionado, não do tpDoc.
        // Preservar todos os demais valores literalmente.
        String ncm = contexto.permiteNcmZero() && ("00".equals(item.NCM()) || "00000000".equals(item.NCM()))
                ? null : item.NCM();

        // 1. Validar e converter os insumos monetários do item. Documento sem
        // Imposto Seletivo (NFS-e): o campo é opcional, tem de ser zero (VSN-035)
        // e não entra no cálculo nem na resposta.
        BigDecimal baseCalculo = validarDecimal(item.baseCalculo(), collector, "baseCalculo",
                CodigoValidacaoSimples.BASE_AUSENTE, CodigoValidacaoSimples.BASE_INVALIDA);

        // A origem executa a validação estrutural de NCM/NBS depois da base.
        // VSN-027 permanece prioritário; a validação do IS segue sua própria regra.
        boolean estruturaNomenclaturaValida = validacaoNomenclaturaSimples.validarEstrutural(ncm, item.NBS(),
                contexto.data(), collector, index);

        BigDecimal valorIs = contexto.admiteImpostoSeletivo()
                ? validarDecimal(item.valorImpostoSeletivo(), collector, "valorImpostoSeletivo",
                        CodigoValidacaoSimples.IS_INVALIDO, CodigoValidacaoSimples.IS_INVALIDO)
                : validarImpostoSeletivoNaoAdmitido(item.valorImpostoSeletivo(), collector);

        // 2. Resolver o tipo de receita bruta do item: o código tpRBSN vem do
        // cClassTrib (vigente na data, admissível para o tpDoc) e o EFEITO do
        // código vem da tabela de tipos de receita bruta (linha do vocabulário
        // do documento, se houver; senão, a padrão).
        ResolucaoClassificacaoItem resolucaoClassificacao = resolverTipoReceitaBruta(item.cClassTrib(), contexto,
                collector);
        TipoReceitaBrutaSimples tipoReceita = resolucaoClassificacao == null
                ? null : resolucaoClassificacao.tipoReceita();

        validacaoNomenclaturaSimples.validarPorClassificacao(ncm, item.NBS(), contexto.data(),
                contexto.siglaDfe(), resolucaoClassificacao == null ? null : resolucaoClassificacao.classificacao(),
                estruturaNomenclaturaValida, collector, index);

        boolean calculaTributos = !Boolean.FALSE.equals(contexto.regime().getCalculaTributos());
        AtividadeItemSimples atividade = new AtividadeItemSimples(null, null, false);
        AliquotasItemSimples aliquotas = new AliquotasItemSimples(null, null, false, null);
        if (calculaTributos) {
            // 3. Validar o código de atividade (cAtivSN). Código inválido NÃO é erro:
            // leva ao estado NAO_CALCULADO.
            atividade = validarCAtivSN(item.cAtivSN(), tipoReceita, contexto, collector);

            // 4. Alíquotas obrigatórias condicionais: exigidas apenas quando o
            // cálculo as usa — cAtivSN válido, tipo de receita bruta que exige
            // alíquota (parametrizado na tabela de tipos) e regime que gera o
            // tributo (para regime que não gera IBS, como o CRT=2, pIBSSN nunca é
            // exigida: é apenas insumo da transformação de compra governamental,
            // quando informada).
            boolean aliquotasExigidas = atividade.valida() && tipoReceita != null
                    && Boolean.TRUE.equals(tipoReceita.getExigeAliquota());
            boolean pIbsExigida = aliquotasExigidas && Boolean.TRUE.equals(contexto.regime().getGeraIbs());
            aliquotas = resolvedor.resolver(
                    new DadosAliquotasItem(index, atividade, tipoReceita, aliquotasExigidas, pIbsExigida), collector);
        }
        // Regime que não calcula tributos (SOMENTE_RECEITA): nem cAtivSN nem
        // alíquotas são exigidos; o item sai NAO_CALCULADO, só com a receita bruta.

        // 5. Se houver erros de validação, não calcula.
        if (collector.hasErros()) {
            return new ItemProcessadoSimples(new ObjetoSimplesDomain(nObj != null ? nObj : index + 1,
                    collector.getEstado(), null, collector.getErros()), aliquotas.resolucao());
        }

        // 6. Calcular, com todos os parâmetros já resolvidos (o serviço de
        // cálculo é puro, sem acesso a banco). A compra governamental
        // (tpEnteGov e fator de transição) vem do contexto da operação — nível
        // de nota, como no grupo gCompraGov do DF-e — e é aplicada às alíquotas
        // deste item.
        // Motivo do NAO_CALCULADO do item (cAtivSN ou anexo); o do regime é do
        // serviço de cálculo, que lhe dá precedência.
        MotivoEstadoSimplesDomain motivoNaoCalculo = calculaTributos
                ? motivoNaoCalculo(atividade, aliquotas, contexto)
                : null;
        try {
            InsumoCalculoSimples insumo = new InsumoCalculoSimples(
                    contexto.regime(), tipoReceita, motivoNaoCalculo,
                    baseCalculo, valorIs, aliquotas.pIBSSN(), aliquotas.pCBSSN(),
                    contexto.tpEnteGov(), contexto.fatorTransicaoCompraGov(), contexto.casasAliquotaCalculo());

            ResultadoCalculoSimples resultado = calculoService.calcularItem(insumo);

            return new ItemProcessadoSimples(new ObjetoSimplesDomain(nObj, resultado.estadoItem(),
                    resultado.motivoEstado(), resultado.tribCalc(), null), aliquotas.resolucao());
        } catch (Exception ex) {
            log.debug("Erro ao processar item {} (índice {}): {}",
                    nObj != null ? nObj : index + 1, index, ex.getMessage());
            collector.addFalhaTecnica(ex);
            return new ItemProcessadoSimples(new ObjetoSimplesDomain(nObj != null ? nObj : index + 1,
                    collector.getEstado(), null, collector.getErros()), aliquotas.resolucao());
        }
    }

    /**
     *
     * Motivo pelo qual o item, num regime que calcula tributos, não é
     * calculável — na ordem: cAtivSN ausente, cAtivSN sem atividade vigente,
     * atividade não admitida no tpDoc e, com a atividade válida, anexo sem a
     * CBS na Tabela de Alíquotas Efetivas do Simples Nacional (só no
     * assistido). O motivo do regime ({@code SOMENTE_RECEITA}) é do serviço de
     * cálculo, que lhe dá precedência.
     *
     * @return o motivo com a descrição resolvida, ou {@code null} quando o item é calculável
     *
     */
    static MotivoEstadoSimplesDomain motivoNaoCalculo(AtividadeItemSimples atividade,
            AliquotasItemSimples aliquotas, ContextoItemSimples contexto) {
        if (atividade.cAtivSN() == null) {
            return MotivoNaoCalculoSimplesEnum.CATIVSN_AUSENTE.motivo();
        }
        if (atividade.atividade() == null) {
            return MotivoNaoCalculoSimplesEnum.CATIVSN_DESCONHECIDO.motivo(atividade.cAtivSN());
        }
        if (!atividade.admissivel()) {
            String documento = contexto.siglaDfe() != null ? contexto.siglaDfe() : "informado";
            return MotivoNaoCalculoSimplesEnum.CATIVSN_INADMISSIVEL_NO_DOCUMENTO.motivo(atividade.cAtivSN(),
                    documento, contexto.tpDoc());
        }
        if (aliquotas.naoCalcular()) {
            String anexo = aliquotas.resolucao() != null ? aliquotas.resolucao().anexoAplicado() : null;
            return MotivoNaoCalculoSimplesEnum.ANEXO_SEM_ALIQUOTA_CBS.motivo(anexo != null ? anexo : "aplicado");
        }
        return null;
    }

    /**
     * cAtivSN como o cálculo o usa: sem espaços nas pontas e, nos endpoints
     * novos, com o zero à esquerda quando chega com um só dígito ("7" ⇒ "07";
     * na DPS da NFS-e o campo {@code cAtvSN} é numérico). {@code null} quando
     * ausente ou em branco.
     */
    public static String normalizarCAtivSN(String cAtivSN, boolean completarZero) {
        if (cAtivSN == null || cAtivSN.isBlank()) {
            return null;
        }
        String codigo = cAtivSN.trim();
        return completarZero && UM_DIGITO.matcher(codigo).matches() ? "0" + codigo : codigo;
    }

    /**
     *
     * Verificação PRIORITÁRIA de implementação do cClassTrib — a primeira
     * consulta de banco executada para o item, antes de qualquer outra
     * validação (base, tpRBSN, admissibilidade no tpDoc, cAtivSN, alíquotas).
     *
     * <p>Espelha o regime geral ({@code CalculoCbsIbsService}), que lança
     * {@code ErroFaltaImplementacaoException} (CAL-007, estado
     * {@code NAO_IMPLEMENTADO}) quando o tratamento tributário vigente da
     * classificação possui o indicador de ajuste ({@code TRTR_IN_POSSUI_AJUSTE}):
     * na Calculadora, esse indicador marca a classificação cujo tratamento ainda
     * não foi implementado. Aqui a consequência é a mesma — estado
     * {@code NAO_IMPLEMENTADO}, erro VSN-027 apontando {@code cClassTrib} — sem
     * exceção, pelo coletor de erros do item.
     *
     * <p>A consulta restringe ao grupo da CBS (tributo 2) e filtra as três
     * tabelas pela data do fato gerador. cClassTrib ausente não é verificado
     * (segue para VSN-020); código desconhecido ou fora de vigência conta zero
     * (segue para VSN-021).
     *
     * @return {@code true} quando o item foi marcado como {@code NAO_IMPLEMENTADO}
     *         (o chamador deve encerrar o processamento do item)
     *
     */
    private boolean cClassTribNaoImplementado(String cClassTrib, ContextoItemSimples contexto,
            ItemCollectorSimples collector) {
        if (cClassTrib == null || cClassTrib.isBlank()) {
            return false;
        }
        String codigo = cClassTrib.trim();

        if (classificacaoTributariaRepository.contarTratamentosNaoImplementados(codigo, contexto.data()) == 0) {
            return false;
        }

        collector.addNaoImplementado(CodigoValidacaoSimples.CCLASSTRIB_NAO_IMPLEMENTADO, CAMPO_CCLASSTRIB,
                String.format("A classificação tributária '%s' ainda não possui tratamento implementado no "
                        + "cálculo do Simples Nacional na data '%s'. Essa funcionalidade está prevista no "
                        + "roadmap e será disponibilizada em versão futura.", codigo, contexto.data()));
        return true;
    }

    /**
     *
     * Resolve o tipo de receita bruta do item em dois passos:
     * <ol>
     * <li>o código tpRBSN vem do cClassTrib informado, vigente na data do fato
     * gerador e admissível para o tpDoc da operação (via
     * {@code TIPO_DFE_CLASSIFICACAO});</li>
     * <li>o efeito do código (descrição, fator de receita, exigência de
     * alíquota, etc.) vem da tabela {@code TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL},
     * também pela vigência da data — a linha padrão no clássico; a do
     * vocabulário do documento, se houver, nos endpoints novos.</li>
     * </ol>
     *
     * <p>Como {@code CLTR_CD} não é único (os grupos do Imposto Seletivo e da
     * CBS colidem em códigos), a resolução do passo 1 é restrita ao grupo da
     * CBS (tributo 2) — o filtro vive nas consultas do repositório.
     *
     * <p>Erros de item acumulados: cClassTrib ausente (VSN-020), desconhecido ou
     * fora de vigência (VSN-021), incompatível com o tpDoc (VSN-022) e código de
     * tpRBSN sem parametrização de efeito vigente (VSN-026).
     *
     * @return a classificação CBS/IBS e o tipo de receita resolvidos, ou {@code null}
     *         quando a classificação está ausente/desconhecida/incompatível
     *
     */
    private ResolucaoClassificacaoItem resolverTipoReceitaBruta(String cClassTrib,
            ContextoItemSimples contexto, ItemCollectorSimples collector) {
        if (cClassTrib == null || cClassTrib.isBlank()) {
            collector.addErroValidacao(CodigoValidacaoSimples.CCLASSTRIB_AUSENTE, CAMPO_CCLASSTRIB,
                    "O código de classificação tributária (cClassTrib) é obrigatório e não foi informado.");
            return null;
        }
        String codigo = cClassTrib.trim();

        List<ClassificacaoTributariaSimples> vigentes =
                classificacaoTributariaRepository.buscarVigentes(codigo, contexto.data());
        if (vigentes.isEmpty()) {
            collector.addErroValidacao(CodigoValidacaoSimples.CCLASSTRIB_DESCONHECIDO, CAMPO_CCLASSTRIB,
                    String.format("O cClassTrib '%s' não existe ou não está vigente na data '%s': "
                            + "o tpRBSN não pôde ser determinado.", codigo, contexto.data()));
            return null;
        }
        ClassificacaoTributariaSimples classificacao = vigentes.get(0);

        if (classificacaoTributariaRepository.contarVinculosComTpDoc(codigo, contexto.tpDoc(),
                contexto.data()) == 0) {
            collector.addErroValidacao(CodigoValidacaoSimples.CCLASSTRIB_INCOMPATIVEL_TPDOC, CAMPO_CCLASSTRIB,
                    String.format("O cClassTrib '%s' não é admissível para o tipo de documento fiscal %d.",
                            codigo, contexto.tpDoc()));
            return null;
        }

        int tpRBSN = classificacao.getTpRBSN();
        TipoReceitaBrutaSimples tipoReceita = (contexto.vocabularioTipoReceita() == null
                ? parametroService.buscarTipoReceitaBruta(tpRBSN, contexto.data())
                : parametroService.buscarTipoReceitaBruta(tpRBSN, contexto.vocabularioTipoReceita(), contexto.data()))
                .orElse(null);
        if (tipoReceita == null) {
            collector.addErroValidacao(CodigoValidacaoSimples.PARAMETRIZACAO_INDISPONIVEL, CAMPO_CCLASSTRIB,
                    String.format("Não há parametrização de efeito vigente para o tipo de receita bruta "
                            + "tpRBSN=%d na data '%s'.", tpRBSN, contexto.data()));
        }
        return new ResolucaoClassificacaoItem(classificacao, tipoReceita);
    }

    /**
     *
     * Valida o código de atividade do Simples Nacional (cAtivSN).
     *
     * <p>Ausente, desconhecido, fora de vigência ou inadmissível para o tpDoc
     * NÃO é erro: a atividade volta não válida e o item segue para o estado
     * {@code NAO_CALCULADO}, reportando apenas a receita bruta e o tipo de
     * receita (NT 2026.005, seção 5.3.4, Observação 2).
     *
     * <p>A admissibilidade por documento é POR ATIVIDADE, via associativa
     * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}, com semântica de WHITELIST
     * (mundo fechado): a atividade é admissível no tpDoc da operação se e
     * somente se existe vínculo vigente na data entre a atividade e o tpDoc;
     * sem vínculo vigente algum, a atividade não é admissível em documento
     * nenhum. A admissibilidade é um fato afirmativo publicado por NT —
     * ausência de publicação significa ausência de permissão. A associativa
     * não substitui a whitelist global de tpDoc
     * ({@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}) — são camadas distintas.
     *
     * <p>A única exceção que É erro (VSN-023, espelho da regra UB14-90 da NT,
     * msg 1277): atividade marcada como de operações não tributáveis (coluna
     * {@code ATSN_IN_NAO_TRIBUTAVEL}; hoje, o código '90') combinada com tipo de
     * receita bruta vedado para não tributáveis (coluna
     * {@code TRBS_IN_VEDADO_NAO_TRIBUTAVEL}; tpRBSN 1 a 5, que declaram receita)
     * — contradição entre "não tributável" e um código que declara receita.
     *
     * @return a atividade do item (vigente ou não, admissível ou não)
     *
     */
    private AtividadeItemSimples validarCAtivSN(String cAtivSN, TipoReceitaBrutaSimples tipoReceita,
            ContextoItemSimples contexto, ItemCollectorSimples collector) {
        String codigo = normalizarCAtivSN(cAtivSN, contexto.normalizarCAtivSN());
        if (codigo == null) {
            return new AtividadeItemSimples(null, null, false);
        }

        List<AtividadeSimplesNacional> atividades =
                atividadeCalculoRepository.buscarVigentesPorCAtivSN(codigo, contexto.data());
        if (atividades.isEmpty()) {
            return new AtividadeItemSimples(codigo, null, false);
        }
        AtividadeSimplesNacional atividade = atividades.get(0);

        if (atividadeCalculoRepository.contarVinculosComTpDoc(atividade.getId(),
                contexto.tpDoc(), contexto.data()) == 0) {
            return new AtividadeItemSimples(codigo, atividade, false);
        }

        if (Boolean.TRUE.equals(atividade.getNaoTributavel()) && tipoReceita != null
                && Boolean.TRUE.equals(tipoReceita.getVedadoNaoTributavel())) {
            collector.addErroValidacao(CodigoValidacaoSimples.CATIVSN_INCOMPATIVEL_CCLASSTRIB, "cAtivSN",
                    String.format("O código de atividade '%s' (Operações não Tributáveis) é incompatível com "
                            + "cClassTrib de tpRBSN=%d.", codigo, tipoReceita.getCodigo()));
        }
        return new AtividadeItemSimples(codigo, atividade, true);
    }

    /**
     *
     * Valida e converte uma alíquota (pontos percentuais) recebida como texto.
     * A ausência só é erro quando a alíquota é exigida pelo cálculo; valor
     * informado negativo ou fora do formato (até 3 dígitos inteiros e até 34
     * casas, sem expoente) é sempre erro. A alíquota aceita é usada no cálculo
     * com todas as casas.
     *
     * @return o {@link BigDecimal} convertido ou {@code null} quando ausente ou com erro
     *
     */
    public static BigDecimal validarAliquota(String valor, boolean exigida, ItemCollectorSimples collector,
            String campo, CodigoValidacaoSimples codigoAusente, CodigoValidacaoSimples codigoInvalido) {
        if (valor == null || valor.isBlank()) {
            if (exigida) {
                collector.addErroValidacao(codigoAusente, campo,
                        "A alíquota é exigida para o cálculo do item e não foi informada.");
            }
            return null;
        }
        return converterNoFormato(valor, FORMATO_ALIQUOTA, DESCRICAO_FORMATO_ALIQUOTA, collector, campo,
                codigoInvalido);
    }

    /**
     *
     * Valida e converte um valor monetário obrigatório e não negativo recebido
     * como texto, no tipo 13v2 do leiaute (até 13 dígitos inteiros e até 2
     * casas, sem expoente). Acumula o erro adequado no collector, caso seja
     * ausente ({@code codigoAusente}), fora do formato ou negativo
     * ({@code codigoInvalido}).
     *
     * @return o {@link BigDecimal} convertido ou {@code null} em caso de erro
     *
     */
    private static BigDecimal validarDecimal(String valor, ItemCollectorSimples collector, String campo,
            CodigoValidacaoSimples codigoAusente, CodigoValidacaoSimples codigoInvalido) {
        if (valor == null || valor.isBlank()) {
            collector.addErroValidacao(codigoAusente, campo, "O campo é obrigatório e não foi informado.");
            return null;
        }
        return converterNoFormato(valor, FORMATO_VALOR, DESCRICAO_FORMATO_VALOR, collector, campo, codigoInvalido);
    }

    /**
     * Imposto Seletivo em documento que não o tem (NFS-e): ausente ou zero é
     * aceito; fora do formato é VSN-003; outro valor é VSN-035. Volta sempre
     * {@code null}: o IS não entra no cálculo nem na resposta.
     */
    private static BigDecimal validarImpostoSeletivoNaoAdmitido(String valor, ItemCollectorSimples collector) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        BigDecimal convertido = converterNoFormato(valor, FORMATO_VALOR, DESCRICAO_FORMATO_VALOR, collector,
                "valorImpostoSeletivo", CodigoValidacaoSimples.IS_INVALIDO);
        if (convertido != null && convertido.signum() != 0) {
            collector.addErroValidacao(CodigoValidacaoSimples.IS_NAO_ADMITIDO, "valorImpostoSeletivo",
                    String.format("O documento não tem Imposto Seletivo: o valor deve ser zero ou não ser "
                            + "informado, recebido: '%s'.", valor));
        }
        return null;
    }

    /**
     *
     * Converte um número recebido como texto SOMENTE se ele estiver no formato
     * permitido; senão acumula {@code codigoInvalido} no collector. A
     * verificação do formato precede a conversão: {@code new BigDecimal} aceita
     * notação científica, e um expoente grande (ex.: {@code 1E+100000000})
     * esgotaria a memória da aplicação no cálculo.
     *
     * @return o {@link BigDecimal} convertido ou {@code null} em caso de erro
     *
     */
    private static BigDecimal converterNoFormato(String valor, Pattern formato, String descricaoFormato,
            ItemCollectorSimples collector, String campo, CodigoValidacaoSimples codigoInvalido) {
        String texto = valor.trim();
        if (formato.matcher(texto).matches()) {
            return new BigDecimal(texto);
        }
        if (texto.startsWith("-") && formato.matcher(texto.substring(1)).matches()) {
            collector.addErroValidacao(codigoInvalido, campo,
                    String.format("O valor não pode ser negativo, recebido: '%s'.", valor));
        } else {
            collector.addErroValidacao(codigoInvalido, campo,
                    String.format("O valor não é um número decimal válido, recebido: '%s'. Informe %s.", valor,
                            descricaoFormato));
        }
        return null;
    }
}
