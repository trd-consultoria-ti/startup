package br.gov.serpro.rtc.shared.exceptionhandler.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Detalhe de erro individual da API, conforme RFC 7807/9457 — a estrutura
 * COMPARTILHADA da propriedade {@code errors[]} de qualquer {@code ProblemDetail}
 * do projeto (ex-{@code ErroDetalheSimples}, generalizado).
 *
 * <p>O campo {@code pointer} segue a RFC 6901 (JSON Pointer).
 *
 * <p>{@code @JsonInclude(NON_NULL)} omite os campos nulos: erros genéricos sem
 * catálogo próprio (ex.: validação de campos ou de query params) não têm
 * {@code code} nem {@code type}; os erros {@code VSN-} do Simples Nacional
 * sempre preenchem ambos — o catálogo de códigos permanece na feature
 * ({@code CodigoValidacaoSimples}), apenas a estrutura do erro é compartilhada.
 *
 * @param type    URI de consulta ao catálogo do erro (nulo quando não há catálogo)
 * @param title   título curto do erro
 * @param code    código catalogado (ex.: {@code VSN-001}; nulo quando não há catálogo)
 * @param pointer JSON Pointer (RFC 6901) do campo/parâmetro de origem
 * @param detail  mensagem detalhada
 */
@JsonInclude(NON_NULL)
public record ErroDetalhe(String type, String title, String code, String pointer, String detail) {
}
