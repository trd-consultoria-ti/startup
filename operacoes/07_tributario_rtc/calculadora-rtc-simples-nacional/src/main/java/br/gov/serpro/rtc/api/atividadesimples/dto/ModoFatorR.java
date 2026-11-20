package br.gov.serpro.rtc.api.atividadesimples.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Estado da decisão de Fator R de uma atividade do Simples Nacional, dada a
 * configuração de anexos informada na consulta.
 *
 * <p>
 *
 * ISOLADO.
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples} e não depende do
 * pacote {@code simplesnacional} nem do domínio do regime geral.
 *
 */
@Schema(description = "Estado da decisão de Fator R da atividade, dada a configuração de anexos informada.")
public enum ModoFatorR {

    /**
     * A atividade não depende do Fator R ({@code sujeitaFatorR == false}),
     * incluindo a atividade sem anexo (ex.: código 90, operações não tributáveis).
     */
    NAO_APLICAVEL,

    /**
     * Os dois anexos da atividade (sem e com Fator R — para a 09, o Anexo V e o
     * Anexo III, respectivamente) estão na configuração informada: a decisão de
     * Fator R permanece em aberto (opcional).
     */
    OPCIONAL,

    /**
     * O caminho do Fator R (fator "r" igual ou superior a 28%, anexo
     * {@code anexoComFatorR}) não está disponível: apenas o anexo sem Fator R
     * está na configuração informada, ou nenhum dos dois está (nesse caso a
     * atividade já vem com {@code disponivel == false}).
     */
    INDISPONIVEL,

    /**
     * Apenas o anexo com Fator R ({@code anexoComFatorR}, fator "r" igual ou
     * superior a 28%) está na configuração informada: se a atividade for
     * selecionada, o caminho do Fator R é obrigatório.
     */
    OBRIGATORIO
}
