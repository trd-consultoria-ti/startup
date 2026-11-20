package br.gov.serpro.rtc.api.simplesnacional.enumeration;

import br.gov.serpro.rtc.api.simplesnacional.model.output.MotivoEstadoSimplesDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * Motivo pelo qual um item saiu no estado {@code NAO_CALCULADO} (campo
 * {@code motivoEstado} do item): o IBS e a CBS do item não foram calculados e
 * isso NÃO é erro — o item reporta apenas a receita bruta e o tipo de receita.
 *
 * <p>
 * Cada motivo tem o código (contrato estável, para o consumidor decidir) e a
 * descrição pronta para exibição ao usuário; os {@code %s} da descrição são
 * resolvidos por {@link #descrever(Object...)} antes de ir para a resposta.
 *
 * <p>
 * PRECEDÊNCIA (quando mais de um se aplicaria): primeiro o regime
 * ({@link #REGIME_SOMENTE_RECEITA}), depois o cAtivSN
 * ({@link #CATIVSN_AUSENTE}, {@link #CATIVSN_DESCONHECIDO},
 * {@link #CATIVSN_INADMISSIVEL_NO_DOCUMENTO}) e, por fim, o anexo
 * ({@link #ANEXO_SEM_ALIQUOTA_CBS}). No endpoint clássico só ocorrem os
 * motivos de cAtivSN.
 *
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "Motivo do estado NAO_CALCULADO do item.")
public enum MotivoNaoCalculoSimplesEnum {

    /**
     * Regime que não calcula tributos ({@code SOMENTE_RECEITA}): optante que
     * apura o IBS e a CBS pelo regime regular (CRT 1/2 com
     * {@code tpRegimeIBSCBS=1}; NFS-e com {@code regApIBSCBSSN=3}).
     */
    REGIME_SOMENTE_RECEITA("O optante apura o IBS e a CBS pelo regime regular, fora do Simples Nacional; o item "
            + "leva ao Simples apenas a receita bruta."),

    /** cAtivSN nulo ou em branco. */
    CATIVSN_AUSENTE("O código de atividade do Simples Nacional (cAtivSN) não foi informado; sem ele o IBS e a CBS "
            + "do item não são calculados (NT 2026.005, §5.3.3)."),

    /** cAtivSN informado, sem atividade vigente na data do fato gerador. Parâmetro: o código. */
    CATIVSN_DESCONHECIDO("O código de atividade '%s' não existe ou não está vigente na data do fato gerador; o IBS "
            + "e a CBS do item não são calculados."),

    /**
     * Atividade vigente, mas sem vínculo vigente com o tipo de documento.
     * Parâmetros: o código, a sigla do documento e o tpDoc.
     */
    CATIVSN_INADMISSIVEL_NO_DOCUMENTO("A atividade '%s' não é admitida no tipo de documento %s (tpDoc %s); o IBS e "
            + "a CBS do item não são calculados."),

    /**
     * Anexo resolvido, mas a Tabela de Alíquotas Efetivas do Simples Nacional
     * não traz a CBS do par escolhido (dado inconsistente; só no cálculo
     * assistido). Parâmetro: o anexo (I a V).
     */
    ANEXO_SEM_ALIQUOTA_CBS("A Tabela de Alíquotas Efetivas do Simples Nacional não traz a alíquota de CBS do Anexo "
            + "%s para o contribuinte no período; o IBS e a CBS do item não são calculados.");

    /** Texto base da descrição, com os {@code %s} a resolver. */
    private final String descricaoBase;

    /** Descrição pronta para exibição, com os parâmetros resolvidos na ordem dos {@code %s}. */
    public String descrever(Object... parametros) {
        return parametros.length == 0 ? descricaoBase : String.format(descricaoBase, parametros);
    }

    /** O motivo como valor da resposta (código e descrição resolvida). */
    public MotivoEstadoSimplesDomain motivo(Object... parametros) {
        return new MotivoEstadoSimplesDomain(this, descrever(parametros));
    }
}
