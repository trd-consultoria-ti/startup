package br.gov.serpro.rtc.api.simplesnacional.exceptionhandler;

import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;

/**
 * Erro GLOBAL (da operação, HTTP 422) no formato do catálogo {@code VSN-}:
 * o {@code type} aponta para o catálogo de erros e o {@code pointer} para o
 * campo da operação. Usado pelos endpoints não assistido e assistido; o
 * endpoint clássico mantém o seu montador privado, com o mesmo formato.
 */
public final class ErroGlobalSimples {

    private ErroGlobalSimples() {
    }

    /**
     * @param campo caminho do campo a partir da raiz da operação, sem {@code #/}
     *              (ex.: {@code tpDoc}, {@code aliquotasEfetivas/anexoIII/pCBSSN})
     */
    public static ErroDetalhe de(CodigoValidacaoSimples codigo, String campo, String detail, String baseURL) {
        String type = String.format(
                "%s/calculadora/observabilidade/simples-nacional/erros?codigo=%s", baseURL, codigo.getCode());
        return new ErroDetalhe(type, codigo.getTitulo(), codigo.getCode(), "#/" + campo, detail);
    }
}
