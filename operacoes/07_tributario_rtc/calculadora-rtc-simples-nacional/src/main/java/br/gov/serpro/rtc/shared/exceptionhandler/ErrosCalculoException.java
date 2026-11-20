package br.gov.serpro.rtc.shared.exceptionhandler;

import java.util.List;

import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;

/**
 *
 * Exceção que encapsula a lista de erros globais identificados durante o
 * processamento de um cálculo em modo de observabilidade (convertida em
 * resposta HTTP 422 pelo {@code ApiExceptionHandler}, o handler único do
 * projeto).
 *
 * Generalizada a partir da ex-{@code ErrosCalculoSimplesException}: a estrutura
 * de erro é compartilhada por toda a API, enquanto os catálogos de códigos
 * (ex.: {@code VSN-} do Simples Nacional) permanecem em cada feature.
 *
 */
public class ErrosCalculoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<ErroDetalhe> erros;

    public ErrosCalculoException(List<ErroDetalhe> erros) {
        super("Erros de cálculo encontrados");
        this.erros = List.copyOf(erros);
    }

    public List<ErroDetalhe> getErros() {
        return erros;
    }
}
