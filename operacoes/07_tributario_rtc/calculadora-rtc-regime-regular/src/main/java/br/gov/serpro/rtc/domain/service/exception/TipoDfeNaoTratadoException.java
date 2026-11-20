/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.exception;

import br.gov.serpro.rtc.domain.model.entity.TipoDfe;

/**
 * Exceção lançada quando o tipo de documento fiscal (tpDoc) informado na operação não possui tratamento implementado na calculadora.
 */
public class TipoDfeNaoTratadoException extends ValidacaoException {
    
    private static final long serialVersionUID = -2890765270447579325L;
	private static final String MESSAGE = "Tipo de documento fiscal (tpDoc) de código %d (%s) não possui tratamento implementado na calculadora";

    public TipoDfeNaoTratadoException(TipoDfe tipo) {
        super(String.format(MESSAGE, tipo.getTipo(), tipo.getSigla()));
    }

}
