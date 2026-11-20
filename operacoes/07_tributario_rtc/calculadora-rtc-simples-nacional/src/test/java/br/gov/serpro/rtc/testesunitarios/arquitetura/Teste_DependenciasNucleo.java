package br.gov.serpro.rtc.testesunitarios.arquitetura;

import static br.gov.serpro.rtc.testesunitarios.arquitetura.FontesJava.estaNoPacote;
import static br.gov.serpro.rtc.testesunitarios.arquitetura.FontesJava.fontes;
import static br.gov.serpro.rtc.testesunitarios.arquitetura.FontesJava.referencia;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Sentido de dependência do núcleo:
 * {@code simplesnacional → aliquotasimples/atividadesimples}. O pacote
 * {@code aliquotasimples} não depende de {@code simplesnacional}, e
 * {@code atividadesimples} não depende de {@code aliquotasimples}. Exceção
 * anterior a esta regra, mantida: o filtro por {@code cClassTrib} do endpoint
 * de atividades reutiliza a resolução do tpRBSN do cálculo
 * ({@code AtividadeSimplesService → simplesnacional}).
 */
class Teste_DependenciasNucleo {

    private static final String PACOTE_SIMPLES = "br.gov.serpro.rtc.api.simplesnacional";
    private static final String PACOTE_ALIQUOTAS = "br.gov.serpro.rtc.api.aliquotasimples";

    @Test
    void aliquotasNaoDependemDoCalculo() {
        List<String> violacoes = fontes()
                .filter(fonte -> estaNoPacote(fonte, "api/aliquotasimples"))
                .filter(fonte -> referencia(fonte, PACOTE_SIMPLES))
                .map(Path::toString)
                .toList();
        assertTrue(violacoes.isEmpty(), () -> "aliquotasimples não pode depender de simplesnacional: " + violacoes);
    }

    @Test
    void atividadesNaoDependemDasAliquotas() {
        List<String> violacoes = fontes()
                .filter(fonte -> estaNoPacote(fonte, "api/atividadesimples"))
                .filter(fonte -> referencia(fonte, PACOTE_ALIQUOTAS))
                .map(Path::toString)
                .toList();
        assertTrue(violacoes.isEmpty(), () -> "atividadesimples não pode depender de aliquotasimples: " + violacoes);
    }
}
