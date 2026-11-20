package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.MotivoNaoCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ImpostoSeletivoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.MemoriaCalculoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.MotivoEstadoSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.TributosSimplesDomain;
import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Serviço de cálculo do Simples Nacional conforme a NT 2026.005 v1.00
 * (Reforma Tributária do Consumo — Simples Nacional; LC 123/2006, LC 214/2025,
 * LC 227/2026), seções 5.3.2 a 5.3.4.
 *
 * <p>
 * Serviço PURO: recebe no insumo todos os parâmetros já resolvidos e validados
 * pelo serviço de observabilidade (efeito do tipo de receita bruta, matriz de
 * regime, fator de compra governamental, alíquotas efetivas) e não faz nenhum
 * acesso a banco. O comportamento parametrizável vive nas tabelas
 * {@code TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL} e
 * {@code REGIME_APURACAO_SIMPLES_NACIONAL}; aqui ficam apenas as fórmulas
 * fixas da NT, em três eixos legíveis separadamente:
 * <ol>
 * <li>transformação de alíquotas por compra governamental (pipeline de passos
 * nomeados sobre o par pIBSSN/pCBSSN);</li>
 * <li>matriz de regime (decide QUAIS campos são gerados e o destino dos
 * valores);</li>
 * <li>efeito do tipo de receita bruta (decide COMO: fração da receita
 * tributada e devido versus pendente/suspenso).</li>
 * </ol>
 *
 * <p>
 * Não chama nem depende do regime geral. Política numérica toda via
 * {@link ArredondamentoUtils}: a aritmética interna é EXATA (as alíquotas
 * entram com todas as casas, inclusive após a transformação de compra
 * governamental); cada valor de item é arredondado UMA única vez, a 2 casas
 * ({@link ArredondamentoUtils#arredondarValorFiscal}), e o total do item é a
 * soma desses valores fiscais — o total sempre fecha com as parcelas exibidas.
 *
 */
@Slf4j
@Service
public class SimplesNacionalCalculoService {

    /**
     * Insumos já resolvidos, validados e convertidos de um item.
     *
     * @param regime                    linha vigente da matriz de regime
     *                                  ({@code REGIME_APURACAO_SIMPLES_NACIONAL})
     * @param tipoReceitaBruta          linha vigente do efeito do tpRBSN
     *                                  ({@code TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL})
     * @param motivoNaoCalculo          motivo, já resolvido por quem monta o insumo, pelo qual o item não
     *                                  é calculável — cAtivSN ausente, desconhecido ou inadmissível no
     *                                  tpDoc, ou anexo sem a CBS na Tabela de Alíquotas Efetivas do
     *                                  Simples Nacional; {@code null} = item calculável. Não nulo leva ao
     *                                  estado {@code NAO_CALCULADO} (o cálculo não é realizado e isso não
     *                                  é erro), com este motivo
     * @param baseCalculo               base de cálculo do item (sem o vIS somado)
     * @param valorImpostoSeletivo      vIS já calculado (repasse)
     * @param pIBSSN                    alíquota efetiva do Simples para o IBS, em pontos percentuais
     *                                  ({@code null} quando dispensada/não informada)
     * @param pCBSSN                    alíquota efetiva do Simples para a CBS, em pontos percentuais
     *                                  ({@code null} quando dispensada/não informada)
     * @param tpEnteGov                 tipo de ente governamental adquirente ({@code null} quando não
     *                                  informado) — dado da OPERAÇÃO (tag {@code gCompraGov/tpEnteGov} da
     *                                  identificação da nota, grupo B/ide do DF-e), o mesmo para todos os
     *                                  itens; a transformação de alíquotas que ele dispara é aplicada por item
     * @param fatorTransicaoCompraGov   fator (fração) do deslocamento de alíquotas CBS→IBS, já resolvido
     *                                  pela vigência da data do fato gerador (também dado da operação);
     *                                  exigido apenas quando {@code tpEnteGov} é 2, 3 ou 4 e há alíquota a
     *                                  transformar
     * @param casasAliquotaCalculo      casas decimais da alíquota usada no cálculo, arredondada DEPOIS da
     *                                  transformação de compra governamental (NFS-e: 2); {@code null} =
     *                                  precisão integral
     */
    public record InsumoCalculoSimples(
            RegimeApuracaoSimples regime,
            TipoReceitaBrutaSimples tipoReceitaBruta,
            MotivoEstadoSimplesDomain motivoNaoCalculo,
            BigDecimal baseCalculo,
            BigDecimal valorImpostoSeletivo,
            BigDecimal pIBSSN,
            BigDecimal pCBSSN,
            Integer tpEnteGov,
            BigDecimal fatorTransicaoCompraGov,
            Integer casasAliquotaCalculo) {

        /** Insumo com a precisão integral da alíquota (endpoint clássico e família CRT). */
        public InsumoCalculoSimples(RegimeApuracaoSimples regime, TipoReceitaBrutaSimples tipoReceitaBruta,
                MotivoEstadoSimplesDomain motivoNaoCalculo, BigDecimal baseCalculo, BigDecimal valorImpostoSeletivo,
                BigDecimal pIBSSN, BigDecimal pCBSSN, Integer tpEnteGov, BigDecimal fatorTransicaoCompraGov) {
            this(regime, tipoReceitaBruta, motivoNaoCalculo, baseCalculo, valorImpostoSeletivo, pIBSSN, pCBSSN,
                    tpEnteGov, fatorTransicaoCompraGov, null);
        }
    }

    /**
     * Resultado do cálculo de um item: estado ({@code CALCULADO} ou
     * {@code NAO_CALCULADO}), o motivo do {@code NAO_CALCULADO} ({@code null}
     * no {@code CALCULADO}) e os tributos correspondentes.
     */
    public record ResultadoCalculoSimples(EstadoItemSimplesEnum estadoItem, MotivoEstadoSimplesDomain motivoEstado,
            TributosSimplesDomain tribCalc) {
    }

    /** Par de alíquotas (pontos percentuais) que atravessa o pipeline de transformação. */
    private record ParAliquotas(BigDecimal pIBSSN, BigDecimal pCBSSN) {
    }

    /** Valor de um tributo repartido entre devido e pendente/suspenso. */
    private record ValorTributo(BigDecimal devido, BigDecimal pendenteSuspenso) {
    }

    /**
     *
     * PONTO ÚNICO DE DECISÃO DE FLUXO DE CÁLCULO.
     *
     * <p>
     * Executa, nesta ordem: receita bruta do item (zero quando o tipo de receita
     * não compõe receita), curto-circuito de regime que não calcula tributos ou
     * de item não calculável (estado {@code NAO_CALCULADO}, sem erro, com o
     * motivo — precedência: o regime primeiro, depois o motivo do insumo),
     * transformação de alíquotas por compra governamental, arredondamento da
     * alíquota às casas do documento (quando ele as exige) e, por fim, a matriz
     * de regime combinada com o efeito do tipo de receita bruta.
     *
     * <p>
     * EXTENSIBILIDADE: quando novos fatores fizerem parte da entrada e exigirem
     * fluxos de cálculo distintos, ramifique AQUI.
     *
     * Não espalhe a regra por outros pontos; concentre-a neste método.
     *
     * @param insumo insumos já resolvidos, validados e convertidos
     * @return estado e tributos calculados do item
     *
     */
    public ResultadoCalculoSimples calcularItem(InsumoCalculoSimples insumo) {
        TipoReceitaBrutaSimples tipoReceita = insumo.tipoReceitaBruta();

        // Receita bruta do item (NT 2026.005, campo P26): zero quando o tipo de
        // receita não compõe receita bruta; caso contrário baseCalculo, somada
        // ao vIS quando a linha vigente do tpRBSN determina que o IS compõe a
        // receita bruta (TRBS_IN_IS_COMPOE_RECEITA, resolvida pela data do fato
        // gerador — carga oficial: 0 em TODAS as vigências, o vIS fica fora da
        // receita bruta). Divergência deliberada com o texto da NT, que soma o
        // vIS incondicionalmente; a coluna permanece como alavanca de
        // parametrização por vigência para mudanças legais futuras — ver o
        // comentário da coluna no script de carga da tabela.
        boolean isCompoeReceita = Boolean.TRUE.equals(tipoReceita.getImpostoSeletivoCompoeReceita());
        BigDecimal vRBSNitem = Boolean.TRUE.equals(tipoReceita.getCompoeReceita())
                ? ArredondamentoUtils.arredondarValorFiscal(ArredondamentoUtils.totalizar(isCompoeReceita
                        ? Stream.of(insumo.baseCalculo(), insumo.valorImpostoSeletivo()).filter(Objects::nonNull)
                        : Stream.of(insumo.baseCalculo())))
                : BigDecimal.ZERO;

        // Regime que não calcula tributos (SOMENTE_RECEITA: optante pelo regime
        // regular do IBS e da CBS — o evento leva só a receita bruta) ou item não
        // calculável (cAtivSN ausente ou inválido — NT 2026.005, seção 5.3.4,
        // Observação 2, e observação da seção 5.3.3 —, ou anexo sem a CBS):
        // cálculo não realizado (NÃO é erro) — o item reporta apenas a receita
        // bruta e o tipo de receita, com o motivo. O regime tem precedência.
        MotivoEstadoSimplesDomain motivo = Boolean.FALSE.equals(insumo.regime().getCalculaTributos())
                ? MotivoNaoCalculoSimplesEnum.REGIME_SOMENTE_RECEITA.motivo()
                : insumo.motivoNaoCalculo();
        if (motivo != null) {
            return new ResultadoCalculoSimples(EstadoItemSimplesEnum.NAO_CALCULADO, motivo,
                    TributosSimplesDomain.builder()
                            .vRBSNitem(vRBSNitem)
                            .tpRBSN(tipoReceita.getCodigo())
                            .tpRBSNDescricao(tipoReceita.getDescricao())
                            .build());
        }

        // Pipeline de transformação de alíquotas por compra governamental
        // (passos nomeados, aplicados em sequência sobre o par pIBSSN/pCBSSN).
        ParAliquotas aliquotas = arredondarAliquotasDoLeiaute(aplicarDeslocamentoCbsIbs(
                aplicarRedutorCompraGovernamental(new ParAliquotas(insumo.pIBSSN(), insumo.pCBSSN())),
                insumo.tpEnteGov(), insumo.fatorTransicaoCompraGov()), insumo.casasAliquotaCalculo());

        // Matriz de regime × efeito do tipo de receita bruta.
        BigDecimal fatorReceita = tipoReceita.getFatorReceita();
        boolean pendenteSuspenso = Boolean.TRUE.equals(tipoReceita.getPendenteSuspenso());

        // Base de cálculo de IBS/CBS do item: a fração da receita bruta
        // efetivamente tributada (vRBSNitem × TRBS_FATOR_RECEITA). Zero quando
        // o fator anula a receita (tpRBSN 0, 2, 3 e 9). Gerada apenas para item
        // CALCULADO — o NAO_CALCULADO reporta só vRBSNitem/tpRBSN (acima). É
        // apresentação: o tributo NÃO incide sobre o vBC arredondado, e sim
        // sobre vRBSNitem × fator exato (fórmulas P29 a P33 da NT).
        BigDecimal vBC = ArredondamentoUtils.arredondarValorFiscal(
                ArredondamentoUtils.multiplicar(vRBSNitem, fatorReceita));

        // Valores fiscais: o único arredondamento de cada valor (2 casas).
        BigDecimal valorIbs = ArredondamentoUtils.arredondarValorFiscal(
                valorTributo(vRBSNitem, fatorReceita, aliquotas.pIBSSN()));
        BigDecimal valorCbs = ArredondamentoUtils.arredondarValorFiscal(
                valorTributo(vRBSNitem, fatorReceita, aliquotas.pCBSSN()));

        // Quando o regime não gera IBS (CRT=2), os campos de IBS não saem na
        // resposta: o IBS desse contribuinte é declarado pelo emitente no
        // gIBSCBS da nota (regime regular), fora deste endpoint.
        boolean geraIbs = Boolean.TRUE.equals(insumo.regime().getGeraIbs());
        ValorTributo ibs = geraIbs ? repartirPorRegime(insumo.regime(), valorIbs, pendenteSuspenso) : null;
        ValorTributo cbs = repartirPorRegime(insumo.regime(), valorCbs, pendenteSuspenso);

        // Imposto Seletivo: repassar o valor recebido (sem recalcular). Nulo no
        // documento que não tem IS (NFS-e): o grupo não sai na resposta.
        BigDecimal valorIs = insumo.valorImpostoSeletivo();

        // Total do item: somente os valores DEVIDOS (pendente/suspenso fica fora) + IS,
        // somados já como valores fiscais (soma exata, sem novo arredondamento).
        BigDecimal totalItem = ArredondamentoUtils.totalizar(
                Stream.of(ibs != null ? ibs.devido() : null, cbs.devido(), valorIs).filter(Objects::nonNull));

        return new ResultadoCalculoSimples(EstadoItemSimplesEnum.CALCULADO, null,
                TributosSimplesDomain.builder()
                        .vRBSNitem(vRBSNitem)
                        .tpRBSN(tipoReceita.getCodigo())
                        .tpRBSNDescricao(tipoReceita.getDescricao())
                        .vBC(vBC)
                        .pIBSSN(geraIbs ? aliquotas.pIBSSN() : null)
                        .vIBSSN(ibs != null ? ibs.devido() : null)
                        .vIBSPendSusp(ibs != null ? ibs.pendenteSuspenso() : null)
                        .pCBSSN(aliquotas.pCBSSN())
                        .vCBSSN(cbs.devido())
                        .vCBSPendSusp(cbs.pendenteSuspenso())
                        .IS(valorIs != null ? new ImpostoSeletivoSimplesDomain(valorIs) : null)
                        .totalItem(totalItem)
                        .memoriaCalculo(new MemoriaCalculoSimplesDomain(
                                memoriaCalculo(tipoReceita, vRBSNitem, vBC, aliquotas.pCBSSN(), cbs),
                                ibs != null ? memoriaCalculo(tipoReceita, vRBSNitem, vBC, aliquotas.pIBSSN(), ibs) : null))
                        .build());
    }

    private String memoriaCalculo(TipoReceitaBrutaSimples tipoReceita, BigDecimal vRBSNitem, BigDecimal vBC,
            BigDecimal aliquota, ValorTributo valor) {
        return String.format("Receita bruta do Simples Nacional de R$ %s (%s). A base de cálculo utilizada é de "
                + "R$ %s, com alíquota efetiva de %s%%. Valor devido de R$ %s e pendente/suspenso de R$ %s.",
                formatarMoeda(vRBSNitem), tipoReceita.getDescricao(), formatarMoeda(vBC),
                formatarPercentual(aliquota), formatarMoeda(valor.devido()), formatarMoeda(valor.pendenteSuspenso()));
    }

    // DecimalFormat não é thread-safe: nova instância a cada chamada
    private static String formatarMoeda(BigDecimal valor) {
        final var formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        formato.setRoundingMode(ArredondamentoUtils.MODO);
        return formato.format(valor != null ? valor : BigDecimal.ZERO);
    }

    private static String formatarPercentual(BigDecimal valor) {
        final var formato = new DecimalFormat("#,##0.00##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        formato.setRoundingMode(ArredondamentoUtils.MODO);
        return formato.format(valor != null ? valor : BigDecimal.ZERO);
    }

    // ─── Pipeline de transformação de alíquotas (compra governamental) ─────────

    /**
     *
     * Passo 1 do pipeline — Redutor de compra governamental (arts. 370 e 472 da
     * LC 214/2025, tag {@code pRedutor} do gCompraGov da NT 2025.002):
     * DELIBERADAMENTE NÃO APLICADO.
     *
     * <p>
     * A NT 2026.005, seção 5.3.3, invoca apenas os arts. 372 e 473 da LC
     * 214/2025 e usa apenas o {@code tpEnteGov}; a tag {@code pRedutor} NÃO
     * participa da fórmula do Evento de Tributação do Simples Nacional
     * (tpEvento 412140). O redutor aplica-se às alíquotas do regime regular —
     * no ecossistema do Simples ele aparece somente no IBS do CRT=2 (gIBSCBS,
     * calculado pelo emitente) e no tpRegimeIBSCBS=1, ambos fora deste
     * endpoint. Este endpoint reproduz o evento oficial: aplicar um redutor que
     * o evento não aplica geraria divergência com o documento oficial. Por isso
     * o {@code pRedutor} também NÃO é aceito no contrato de entrada.
     *
     * <p>
     * PONTO DE INSERÇÃO: caso NT futura determine a aplicação do redutor ao
     * Simples, implemente a regra NESTE método (a assinatura já recebe e
     * devolve o par de alíquotas do pipeline). Não confundir com as tabelas
     * {@code REDUTOR_COMPRA_GOVERNAMENTAL} e {@code TRANSFERENCIA_CBS_ENTE_GOV}
     * do regime geral, que permanecem proibidas para este pacote.
     *
     * @param aliquotas par de alíquotas corrente do pipeline
     * @return o mesmo par, inalterado (identidade deliberada)
     *
     */
    private ParAliquotas aplicarRedutorCompraGovernamental(ParAliquotas aliquotas) {
        return aliquotas;
    }

    /**
     *
     * Passo 2 do pipeline — Deslocamento de alíquotas CBS↔IBS em compras
     * governamentais (arts. 372 e 473 da LC 214/2025; NT 2026.005, seção 5.3.3),
     * aplicado apenas quando {@code tpEnteGov} foi informado:
     * <ul>
     * <li>{@code tpEnteGov=1} (União) — regra ESTRUTURAL e atemporal, sem
     * parâmetro (por isso não está na tabela de transição):
     * {@code pCBS' = pCBS + pIBS; pIBS' = 0};</li>
     * <li>{@code tpEnteGov ∈ {2,3,4}} — com o fator {@code f} (fração) da tabela
     * {@code TRANSICAO_ALIQUOTA_COMPRA_GOV_SIMPLES_NACIONAL}, já resolvido pela
     * vigência da data do fato gerador e recebido no insumo:
     * {@code pIBS' = pIBS + f × pCBS; pCBS' = (1 − f) × pCBS}.</li>
     * </ul>
     *
     * <p>
     * As alíquotas transformadas são as usadas no cálculo E as reportadas na
     * saída ({@code pIBSSN}/{@code pCBSSN}). A transformação é EXATA (ex.:
     * {@code 0,9 × 1.6839379378…}): o cálculo usa a alíquota transformada com
     * todas as casas; a saída a apresenta com 2 a 4 casas.
     *
     * <p>
     * Para regime que não gera IBS (CRT=2), o {@code pIBSSN} recebido é usado
     * APENAS como insumo desta transformação (a fórmula de {@code pCBS'} depende
     * de {@code pIBS}); quando não informado, é tratado como zero. Os campos de
     * IBS continuam suprimidos da saída — a supressão é decidida na matriz de
     * regime.
     *
     */
    private ParAliquotas aplicarDeslocamentoCbsIbs(ParAliquotas aliquotas, Integer tpEnteGov, BigDecimal fator) {
        if (tpEnteGov == null || (aliquotas.pIBSSN() == null && aliquotas.pCBSSN() == null)) {
            return aliquotas;
        }

        BigDecimal pIbs = Objects.requireNonNullElse(aliquotas.pIBSSN(), BigDecimal.ZERO);
        BigDecimal pCbs = Objects.requireNonNullElse(aliquotas.pCBSSN(), BigDecimal.ZERO);

        if (tpEnteGov == 1) {
            // União: pCBS' = pCBS + pIBS; pIBS' = 0.
            return new ParAliquotas(BigDecimal.ZERO,
                    ArredondamentoUtils.totalizar(Stream.of(pCbs, pIbs)));
        }

        Objects.requireNonNull(fator,
                "Fator de transição de compra governamental não resolvido para tpEnteGov " + tpEnteGov);

        // pIBS' = pIBS + f × pCBS; pCBS' = (1 − f) × pCBS.
        BigDecimal pIbsTransformada = ArredondamentoUtils.totalizar(
                Stream.of(pIbs, ArredondamentoUtils.multiplicar(fator, pCbs)));
        BigDecimal pCbsTransformada = ArredondamentoUtils.multiplicar(
                ArredondamentoUtils.subtrair(BigDecimal.ONE, fator), pCbs);

        return new ParAliquotas(pIbsTransformada, pCbsTransformada);
    }

    /**
     *
     * Passo 3 do pipeline — alíquota do LEIAUTE: quando o documento exige um
     * número fixo de casas (NFS-e: 2, campos 1-2V2 do gTribSN), cada alíquota é
     * arredondada a essas casas no modo de {@link ArredondamentoUtils}
     * (HALF_EVEN), DEPOIS da transformação da compra governamental, e o valor
     * sai de {@code base × alíquota arredondada} (AnexoVI da NFS-e, regras 200,
     * 202, 205 e 206). Exceção deliberada à precisão integral. Sem casas fixas,
     * identidade.
     *
     */
    private ParAliquotas arredondarAliquotasDoLeiaute(ParAliquotas aliquotas, Integer casas) {
        if (casas == null) {
            return aliquotas;
        }
        return new ParAliquotas(ArredondamentoUtils.arredondarAliquota(aliquotas.pIBSSN(), casas),
                ArredondamentoUtils.arredondarAliquota(aliquotas.pCBSSN(), casas));
    }

    // ─── Matriz de regime × efeito do tipo de receita bruta ────────────────────

    /**
     * Valor EXATO do tributo: {@code vRBSNitem × fatorReceita × (alíquota / 100)},
     * com o fator de receita (1.0, 0.5 ou 0.0) vindo da tabela de tipos de
     * receita bruta e a alíquota com todas as casas. Sem arredondamento: quem
     * chama aplica o valor fiscal uma única vez. Zero quando o fator anula a
     * receita ou a alíquota é dispensada (nula).
     */
    private BigDecimal valorTributo(BigDecimal vRBSNitem, BigDecimal fatorReceita, BigDecimal aliquota) {
        if (aliquota == null || fatorReceita.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return ArredondamentoUtils.multiplicar(
                ArredondamentoUtils.multiplicar(vRBSNitem, fatorReceita),
                ArredondamentoUtils.percentualParaFracao(aliquota));
    }

    /**
     * Reparte o valor do tributo entre devido e pendente/suspenso aplicando os
     * destinos parametrizados na matriz de regime
     * ({@code REGIME_APURACAO_SIMPLES_NACIONAL}): o valor que o tipo de receita
     * bruta qualificou como devido segue o destino "devido" do regime; o valor
     * qualificado como pendente/suspenso (exportação indireta) segue o destino
     * "pendente/suspenso" — que pode ser ZERO (caso do CRT=3 + tpRegimeIBSCBS=2,
     * literal nas fórmulas P32/P33 da NT 2026.005; intencional, NÃO "corrigir").
     */
    private ValorTributo repartirPorRegime(RegimeApuracaoSimples regime, BigDecimal valor,
            boolean pendenteSuspenso) {
        var destino = pendenteSuspenso ? regime.getDestinoPendenteSuspenso() : regime.getDestinoDevido();
        return switch (destino) {
            case DEVIDO -> new ValorTributo(valor, BigDecimal.ZERO);
            case PENDENTE_SUSPENSO -> new ValorTributo(BigDecimal.ZERO, valor);
            case ZERO -> new ValorTributo(BigDecimal.ZERO, BigDecimal.ZERO);
        };
    }
}
