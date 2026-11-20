/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesunitarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.domain.service.calculotributo.AvaliadorExpressaoAritmetica;

/**
 * Testes unitários para o AvaliadorExpressaoAritmetica
 */
class Teste_AvaliadorExpressaoAritmetica {

    private AvaliadorExpressaoAritmetica avaliador;

    @BeforeEach
    void setup() {
        avaliador = new AvaliadorExpressaoAritmetica();
        // Chamar o método @PostConstruct manualmente para testes
        try {
            var method = AvaliadorExpressaoAritmetica.class.getDeclaredMethod("postConstruct");
            method.setAccessible(true);
            method.invoke(avaliador);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar avaliador", e);
        }
    }

    @Test
    void teste_DivisaoInteiros_DevolveFracao() {
        // Testa que 5/100 resulta em 0.05 e não em 0
        Map<String, BigDecimal> variables = new HashMap<>();
        
        String expressao = "5/100";
        BigDecimal resultado = avaliador.evaluate(expressao, variables);
        
        assertThat(resultado).as("5/100 deve resultar em 0.05, não em 0 (divisão inteira)")
            .isEqualByComparingTo("0.05");
    }

    @Test
    void teste_ExpressaoComVariaveis() {
        // Testa a expressão completa do problema relatado
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("quantidade", new BigDecimal("7720"));
        variables.put("aliquotaAdRemSecundaria", new BigDecimal("0.1922"));
        
        String expressao = "(5/100)*quantidade*0.4285714*aliquotaAdRemSecundaria";
        BigDecimal resultado = avaliador.evaluate(expressao, variables);
        
        // Cálculo esperado, exato: 0.05 * 7720 * 0.4285714 * 0.1922 = 31.79536930888
        BigDecimal esperado = new BigDecimal("0.05")
            .multiply(new BigDecimal("7720"))
            .multiply(new BigDecimal("0.4285714"))
            .multiply(new BigDecimal("0.1922"));
        
        assertThat(resultado).as("A expressão com 5/100 deve calcular corretamente, sem arredondar")
            .isEqualByComparingTo(esperado);
        assertThat(resultado).isEqualByComparingTo("31.79536930888");
    }

    @Test
    void teste_DivisaoDecimaisExplicitos() {
        // Testa que decimais explícitos sempre funcionaram
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("quantidade", new BigDecimal("7720"));
        variables.put("aliquotaAdRemSecundaria", new BigDecimal("0.1922"));
        
        String expressao = "0.05*quantidade*0.4285714*aliquotaAdRemSecundaria";
        BigDecimal resultado = avaliador.evaluate(expressao, variables);
        
        BigDecimal esperado = new BigDecimal("0.05")
            .multiply(new BigDecimal("7720"))
            .multiply(new BigDecimal("0.4285714"))
            .multiply(new BigDecimal("0.1922"));
        
        assertThat(resultado).as("A expressão com 0.05 deve calcular corretamente, sem arredondar")
            .isEqualByComparingTo(esperado);
    }

    @Test
    void teste_DivisaoSimples() {
        Map<String, BigDecimal> variables = new HashMap<>();
        
        String expressao = "1/2";
        BigDecimal resultado = avaliador.evaluate(expressao, variables);
        
        assertThat(resultado).as("1/2 deve resultar em 0.5").isEqualByComparingTo("0.5");
    }

    @Test
    void teste_DivisaoComplexaComVariaveis() {
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("valor", new BigDecimal("100"));
        
        String expressao = "valor * (3/4)";
        BigDecimal resultado = avaliador.evaluate(expressao, variables);
        
        assertThat(resultado).as("100 * 3/4 deve resultar em 75").isEqualByComparingTo("75");
    }

    @Test
    void teste_DivisaoQueNaoTermina_Usa34DigitosSignificativos() {
        BigDecimal resultado = avaliador.evaluate("1/3", new HashMap<>());

        assertEquals(34, resultado.precision(), "1/3 deve usar o contexto de divisão de 34 dígitos");
    }

    @Test
    void teste_MultiplicacaoNaoArredonda() {
        // 22 casas decimais: a conta exata preserva todas
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("a", new BigDecimal("0.12345678901"));
        variables.put("b", new BigDecimal("0.10987654321"));

        BigDecimal resultado = avaliador.evaluate("a*b", variables);

        assertEquals(new BigDecimal("0.0135650052122251181221"), resultado);
    }

    @Test
    void teste_AliquotaEfetiva_ContaExata() {
        // 8,8123% com redução de 60%: fração exata 0,0352492 (antes, 8 casas: 0,03524920)
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("aliquota", new BigDecimal("0.088123"));
        variables.put("percentualReducao", new BigDecimal("0.60"));
        variables.put("pRedutorCompraGov", BigDecimal.ZERO);

        BigDecimal resultado = avaliador.evaluate("aliquota*(1-percentualReducao)*(1-pRedutorCompraGov/100)", variables);

        assertThat(resultado).isEqualByComparingTo("0.0352492");
    }
}
