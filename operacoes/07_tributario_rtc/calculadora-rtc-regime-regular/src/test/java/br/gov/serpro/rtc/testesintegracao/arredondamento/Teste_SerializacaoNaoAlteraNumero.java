/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.gov.serpro.rtc.api.model.input.OperacaoInput;
import br.gov.serpro.rtc.api.model.roc.ROCDomain;
import br.gov.serpro.rtc.domain.service.CalculadoraService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializar não altera número: cada {@link BigDecimal} guardado no objeto de resposta do regime
 * geral é numericamente igual ao texto que sai no JSON. O objeto de resposta guarda o número
 * final; os serializers só o formatam.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
@DisplayName("Serialização não altera os números da resposta")
class Teste_SerializacaoNaoAlteraNumero {

    private static final int NOTAS = 300;

    @Autowired
    private CalculadoraService calculadoraService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Valor guardado no objeto de resposta é igual ao texto serializado")
    void valorGuardadoIgualAoTextoSerializado() throws Exception {
        final List<String> diferencas = new ArrayList<>();
        final int[] comparados = { 0 };
        for (int i = 0; i < NOTAS; i++) {
            final var nota = GeradorLoteArredondamento.gerarNota(GeradorLoteArredondamento.SEMENTE_PADRAO, i);
            final OperacaoInput operacao = objectMapper.readValue(nota.corpo(), OperacaoInput.class);
            final ROCDomain resposta = calculadoraService.calcularTributos(operacao);
            final JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(resposta));
            comparar(resposta, json, "nota " + i + " $", diferencas, comparados);
        }
        assertThat(comparados[0]).as("quantidade de números comparados").isGreaterThan(NOTAS * 10);
        assertThat(diferencas).as("%d números diferem entre o objeto e o JSON (de %d comparados)",
                diferencas.size(), comparados[0]).isEmpty();
    }

    private static void comparar(Object objeto, JsonNode no, String caminho, List<String> diferencas, int[] comparados)
            throws IllegalAccessException {
        if (objeto == null || no == null || no.isMissingNode() || no.isNull()) {
            return;
        }
        if (objeto instanceof Collection<?> colecao) {
            int i = 0;
            for (Object elemento : colecao) {
                comparar(elemento, no.get(i), caminho + "[" + i + "]", diferencas, comparados);
                i++;
            }
            return;
        }
        for (Class<?> c = objeto.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                if (Modifier.isStatic(campo.getModifiers()) || campo.isSynthetic()) {
                    continue;
                }
                campo.setAccessible(true);
                final Object valor = campo.get(objeto);
                final JsonNode filho = no.get(campo.getName());
                if (valor == null || filho == null || filho.isNull()) {
                    continue;
                }
                final String caminhoCampo = caminho + "." + campo.getName();
                if (valor instanceof BigDecimal guardado) {
                    comparados[0]++;
                    final BigDecimal serializado = new BigDecimal(filho.isString() ? filho.asString() : filho.toString());
                    if (guardado.compareTo(serializado) != 0 && diferencas.size() < 50) {
                        diferencas.add(caminhoCampo + ": guardado " + guardado.toPlainString() + ", serializado "
                                + serializado.toPlainString());
                    }
                } else if (valor instanceof Collection<?>
                        || (!(valor instanceof Enum<?>) && valor.getClass().getName().startsWith("br.gov.serpro.rtc"))) {
                    comparar(valor, filho, caminhoCampo, diferencas, comparados);
                }
            }
        }
    }
}
