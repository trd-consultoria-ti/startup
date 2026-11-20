package br.gov.serpro.rtc.testesunitarios.atividadesimples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.api.atividadesimples.dto.ModoFatorR;
import br.gov.serpro.rtc.api.atividadesimples.service.DerivacaoAnexosSimples;
import br.gov.serpro.rtc.api.atividadesimples.service.DerivacaoAnexosSimples.Derivacao;

/**
 * Testes unitários da regra pura de derivação de disponibilidade das
 * atividades do Simples Nacional ({@link DerivacaoAnexosSimples}), cobrindo
 * toda a tabela de cenários do contrato: atividade 09 (sem Fator R = V; com
 * Fator R = III — LC 123/2006, art. 18, §§ 5º-J e 5º-M), atividade 02
 * (I/null), atividade sem anexo — código 90 (null/null) — e o invariante "não
 * existe atividade só com Fator R".
 */
class Teste_DerivacaoAnexosSimples {

    // -------------------------------------------------------------------------
    // Atividade 09: anexoSemFatorR=V (fator r < 28%), anexoComFatorR=III (fator r >= 28%)
    // -------------------------------------------------------------------------

    private static Derivacao atividade9(Set<String> anexos) {
        return DerivacaoAnexosSimples.derivar("V", "III", anexos);
    }

    @Test
    void teste_Atividade9_AnexosI_II_IndisponivelFaltandoIIIeV() {
        Derivacao derivacao = atividade9(Set.of("I", "II"));
        assertFalse(derivacao.disponivel());
        assertEquals(List.of("III", "V"), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.INDISPONIVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade9_AnexosI_V_DisponivelFatorRIndisponivel() {
        // Só o Anexo V (sem Fator R) configurado: a atividade fica disponível,
        // mas o caminho do Fator R (Anexo III) não.
        Derivacao derivacao = atividade9(Set.of("I", "V"));
        assertTrue(derivacao.disponivel());
        assertEquals(List.of("III"), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.INDISPONIVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade9_AnexosI_III_DisponivelFatorRObrigatorio() {
        // Só o Anexo III (com Fator R) configurado: se selecionada, o caminho
        // do Fator R é obrigatório; o Anexo V é o faltante.
        Derivacao derivacao = atividade9(Set.of("I", "III"));
        assertTrue(derivacao.disponivel());
        assertEquals(List.of("V"), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.OBRIGATORIO, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade9_AnexosIII_V_DisponivelFatorROpcional() {
        Derivacao derivacao = atividade9(Set.of("III", "V"));
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.OPCIONAL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade9_SemParametro_DisponivelFatorROpcional() {
        Derivacao derivacao = atividade9(null);
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.OPCIONAL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade9_ConjuntoVazio_IndisponivelFaltandoIIIeV() {
        Derivacao derivacao = atividade9(Set.of());
        assertFalse(derivacao.disponivel());
        assertEquals(List.of("III", "V"), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.INDISPONIVEL, derivacao.modoFatorR());
    }

    // -------------------------------------------------------------------------
    // Atividade 02: anexoSemFatorR=I, anexoComFatorR=null
    // -------------------------------------------------------------------------

    private static Derivacao atividade2(Set<String> anexos) {
        return DerivacaoAnexosSimples.derivar("I", null, anexos);
    }

    @Test
    void teste_Atividade2_AnexosI_II_DisponivelNaoAplicavel() {
        Derivacao derivacao = atividade2(Set.of("I", "II"));
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade2_AnexosII_III_IndisponivelFaltandoI() {
        Derivacao derivacao = atividade2(Set.of("II", "III"));
        assertFalse(derivacao.disponivel());
        assertEquals(List.of("I"), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_Atividade2_SemParametro_DisponivelNaoAplicavel() {
        Derivacao derivacao = atividade2(null);
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    // -------------------------------------------------------------------------
    // Atividade sem anexo: anexoSemFatorR=null, anexoComFatorR=null
    // (código 90, operações não tributáveis) — sempre disponível
    // -------------------------------------------------------------------------

    @Test
    void teste_AtividadeSemAnexo_SemParametro_SempreDisponivel() {
        Derivacao derivacao = DerivacaoAnexosSimples.derivar(null, null, null);
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_AtividadeSemAnexo_ConjuntoVazio_SempreDisponivel() {
        Derivacao derivacao = DerivacaoAnexosSimples.derivar(null, null, Set.of());
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    @Test
    void teste_AtividadeSemAnexo_ComAnexosConfigurados_SempreDisponivel() {
        Derivacao derivacao = DerivacaoAnexosSimples.derivar(null, null, Set.of("I", "V"));
        assertTrue(derivacao.disponivel());
        assertEquals(List.of(), derivacao.anexosFaltantes());
        assertEquals(ModoFatorR.NAO_APLICAVEL, derivacao.modoFatorR());
    }

    // -------------------------------------------------------------------------
    // Invariante: não existe atividade "só com Fator R"
    // -------------------------------------------------------------------------

    @Test
    void teste_Invariante_AnexoComFatorRSemAnexoSemFatorR_LancaExcecao() {
        assertThrows(IllegalStateException.class,
                () -> DerivacaoAnexosSimples.derivar(null, "III", null));
    }

    // -------------------------------------------------------------------------
    // Ordenação e duplicatas
    // -------------------------------------------------------------------------

    @Test
    void teste_AnexosFaltantes_SempreOrdenadosPorRomano() {
        // Conjunto configurado fora de ordem não altera a ordem canônica da saída:
        // mesmo com anexoSemFatorR=V e anexoComFatorR=III, os faltantes saem em
        // ordem de romano (III antes de V).
        Derivacao derivacao = DerivacaoAnexosSimples.derivar("V", "III", Set.of("IV"));
        assertEquals(List.of("III", "V"), derivacao.anexosFaltantes());
    }
}
