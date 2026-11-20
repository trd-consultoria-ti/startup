/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.calculotributo;

import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.dividir;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.springframework.stereotype.Service;
import org.springframework.util.NumberUtils;

import jakarta.annotation.PostConstruct;

/**
 * Serviço responsável por avaliar com segurança expressões aritméticas usadas
 * nas fórmulas tributárias, preservando precisão com BigDecimal.
 * <p>
 * A conta é exata: o resultado de uma expressão não é arredondado. O arredondamento de cada
 * número publicado é feito uma vez, por quem chama, com {@code ArredondamentoUtils}.
 */
@Service
public class AvaliadorExpressaoAritmetica {
    
    private JexlEngine expressionEngine;
    
    @PostConstruct
    private void postConstruct() {
        // Create a JEXL engine with custom arithmetic that uses BigDecimal
        expressionEngine = new JexlBuilder()
                .sandbox(new JexlSandbox()) /* Empty sandbox (no external access) */
                .strict(true)
                .cache(512)
                .cacheThreshold(1024)
                .arithmetic(new BigDecimalArithmetic(true)) /* Use BigDecimal arithmetic */
                .create();
    }
    
    /**
     * Aritmética do JEXL com a política numérica da Calculadora: todos os números viram
     * BigDecimal (evita a divisão inteira, em que 5/100 daria 0); soma, subtração e
     * multiplicação são exatas; só a divisão, que pode não terminar, usa o contexto de divisão
     * de {@code ArredondamentoUtils}.
     */
    private static class BigDecimalArithmetic extends JexlArithmetic {
        
        public BigDecimalArithmetic(boolean strict) {
            super(strict);
        }
        
        @Override
        public Object add(Object left, Object right) {
            return toBigDecimal(left).add(toBigDecimal(right));
        }

        @Override
        public Object subtract(Object left, Object right) {
            return toBigDecimal(left).subtract(toBigDecimal(right));
        }

        @Override
        public Object multiply(Object left, Object right) {
            return toBigDecimal(left).multiply(toBigDecimal(right));
        }

        @Override
        public Object divide(Object left, Object right) {
            BigDecimal leftBD = toBigDecimal(left);
            BigDecimal rightBD = toBigDecimal(right);
            
            if (rightBD.compareTo(ZERO) == 0) {
                throw new ArithmeticException("Division by zero");
            }
            
            // Única operação que pode não terminar: usa o contexto de divisão da política numérica
            return dividir(leftBD, rightBD);
        }
        
        @Override
        public BigDecimal toBigDecimal(Object value) {
            return switch (value) {
              case BigDecimal bd -> bd;
              case Number n -> new BigDecimal(n.toString());
              default -> throw new ArithmeticException("Cannot convert to BigDecimal: " + value);
            };
        }
    }
    
    public BigDecimal evaluate(String expression, Map<String, BigDecimal> variables) {
        final var exprTrim = expression.trim();
        // Caso 1: expressão é só o nome da variável
        if (variables.containsKey(exprTrim)) {
            // deve estar mapeado
            final var valor = variables.get(exprTrim);
            if (valor != null) {
                return valor;
            }
            throw new RuntimeException("Valor não mapeado corretamente: " + exprTrim);
        }

        // Caso geral: usa JEXL
        final JexlExpression jexp = expressionEngine.createExpression(expression);
        final Object evaluate = jexp.evaluate(new MapContextBigDecimal(variables));
        if (evaluate instanceof Number n) {
            // Conta exata: o resultado não é arredondado aqui. Quem chama arredonda, uma vez, o
            // número que vai ser publicado.
            return NumberUtils.convertNumberToTargetClass(n, BigDecimal.class);
        } else {
            throw new NumberFormatException("Erro ao avaliar expressão: " + evaluate);
        }
    }

    /**
     * Wraps a map in a context.
     * <p>
     * Each entry in the map is considered a variable name, value pair.
     * </p>
     */
    private class MapContextBigDecimal implements JexlContext {

        /**
         * The wrapped variable map.
         */
        private final Map<String, BigDecimal> map;

        /**
         * Creates a MapContext wrapping an existing user provided map.
         *
         * @param vars the variable map
         */
        public MapContextBigDecimal(final Map<String, BigDecimal> vars) {
            map = vars == null ? new HashMap<>() : vars;
        }

        @Override
        public Object get(final String name) {
            return map.get(name);
        }

        @Override
        public boolean has(final String name) {
            return map.containsKey(name);
        }

        @Override
        public void set(final String name, final Object value) {
            if (value instanceof BigDecimal bd) {
                map.put(name, bd);
            }
        }
    }
    
}