package br.gov.serpro.rtc.api.simplesnacional.model.output;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.MotivoNaoCalculoSimplesEnum;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Motivo do estado {@code NAO_CALCULADO} de um item (campo
 * {@code motivoEstado}): o código estável e a descrição já resolvida, pronta
 * para exibição ao usuário. Presente somente no item {@code NAO_CALCULADO}.
 *
 * @param codigo    código do motivo
 * @param descricao descrição para o usuário, com os parâmetros já resolvidos
 *
 */
@JsonInclude(ALWAYS)
@JsonPropertyOrder({ "codigo", "descricao" })
@Schema(description = "Motivo do estado NAO_CALCULADO do item (presente somente nesse estado)")
public record MotivoEstadoSimplesDomain(

        @Schema(description = "Código do motivo. REGIME_SOMENTE_RECEITA: o optante apura o IBS e a CBS pelo regime "
                + "regular; CATIVSN_AUSENTE: cAtivSN não informado; CATIVSN_DESCONHECIDO: cAtivSN inexistente ou "
                + "fora de vigência; CATIVSN_INADMISSIVEL_NO_DOCUMENTO: atividade não admitida no tipo de "
                + "documento; ANEXO_SEM_ALIQUOTA_CBS: a Tabela de Alíquotas Efetivas do Simples Nacional não traz "
                + "a CBS do anexo (só no cálculo assistido)", example = "REGIME_SOMENTE_RECEITA")
        MotivoNaoCalculoSimplesEnum codigo,

        @Schema(description = "Descrição do motivo, pronta para exibição ao usuário", example = "O optante apura o "
                + "IBS e a CBS pelo regime regular, fora do Simples Nacional; o item leva ao Simples apenas a "
                + "receita bruta.")
        String descricao) {

    public MotivoEstadoSimplesDomain {
        Objects.requireNonNull(codigo, "codigo é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatória");
    }
}
