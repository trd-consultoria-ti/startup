package br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.dto;

/**
 * DTO de resposta do endpoint {@code GET /calculadora/observabilidade/simples-nacional/erros?codigo=}.
 *
 * Duplicado deliberadamente do análogo {@code ErroDescricao} do regime geral,
 * para preservar o isolamento entre os pacotes.
 */
public record ErroDescricaoSimples(String codigo, String titulo, String descricao) {
}
