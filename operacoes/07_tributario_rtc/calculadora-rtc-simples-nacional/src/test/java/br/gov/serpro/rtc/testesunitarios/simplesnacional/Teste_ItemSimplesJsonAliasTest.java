package br.gov.serpro.rtc.testesunitarios.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.api.simplesnacional.model.input.ItemSimplesUnificadoInput;

class Teste_ItemSimplesJsonAliasTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aliasMaiusculoNcmContinuaAceitoPorCompatibilidade() throws Exception {
        ItemSimplesUnificadoInput item = objectMapper.readValue("""
                {"numero":1,"NCM":"22030000"}
                """, ItemSimplesUnificadoInput.class);

        assertEquals("22030000", item.NCM());
    }
}
