/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.exception;

import java.math.BigDecimal;

import br.gov.serpro.rtc.shared.util.ArredondamentoUtils;

/**
 * Exceção lançada quando a base de cálculo declarada diverge da base calculada
 * a partir dos componentes informados.
 */
public class BaseCalculoComponentesInconsistenteException extends ValidacaoException {

    private static final long serialVersionUID = 529236725876115L;
    private static final String MESSAGE = "A base de cálculo declarada (%s) diverge da base calculada a partir dos componentes informados (%s)";
    public static final String CODIGO_ERRO = "REG-034";

    public BaseCalculoComponentesInconsistenteException(BigDecimal declarada, BigDecimal calculada) {
        super(String.format(MESSAGE, texto(declarada), texto(calculada)));
    }

    /**
     * Texto do valor na mensagem: com 2 casas quando o valor cabe em 2 casas; com todas as casas
     * quando tem mais, para que a mensagem mostre a divergência que a comparação exata encontrou.
     */
    private static String texto(BigDecimal valor) {
        if (valor.stripTrailingZeros().scale() <= ArredondamentoUtils.ESCALA_VALOR_FISCAL) {
            return ArredondamentoUtils.formatarMoeda(valor);
        }
        return valor.toPlainString();
    }

}