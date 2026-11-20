/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.testesintegracao.arredondamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.gov.serpro.rtc.testesintegracao.arredondamento.GeradorLoteArredondamento.Nota;
import br.gov.serpro.rtc.testesintegracao.arredondamento.VerificadorInvariantesArredondamento.Violacao;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Lote de arredondamento: envia ao endpoint do regime geral as notas do
 * {@link GeradorLoteArredondamento} e confere, em cada resposta serializada, as invariantes do
 * {@link VerificadorInvariantesArredondamento}. Nenhuma nota pode ser recusada e nenhuma
 * invariante pode ser violada.
 * <p>
 * Propriedades de sistema (todas opcionais):
 * <ul>
 *   <li>{@code arredondamento.lote.notas}: quantidade de notas (padrão
 *       {@value GeradorLoteArredondamento#TAMANHO_PADRAO}; use 10000 para o lote completo);</li>
 *   <li>{@code arredondamento.lote.semente}: semente do gerador;</li>
 *   <li>{@code arredondamento.lote.saida}: diretório em que as respostas ({@code lote.jsonl.gz}) e
 *       a contagem de violações ({@code violacoes.txt}, com o detalhe em
 *       {@code violacoes-detalhe.txt.gz}) são gravadas, para comparação entre versões.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
@DisplayName("Lote de arredondamento do regime geral")
class Teste_LoteArredondamento {

    private static final int MAX_VIOLACOES_EXIBIDAS = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Nenhuma nota do lote é recusada nem viola invariante de arredondamento")
    void loteNaoViolaInvariantes() throws Exception {
        final int quantidade = Integer.getInteger("arredondamento.lote.notas", GeradorLoteArredondamento.TAMANHO_PADRAO);
        final long semente = Long.getLong("arredondamento.lote.semente", GeradorLoteArredondamento.SEMENTE_PADRAO);
        final String saida = System.getProperty("arredondamento.lote.saida");

        final Map<String, Integer> violacoesPorInvariante = new TreeMap<>();
        final Map<String, Integer> notasPorInvariante = new TreeMap<>();
        final List<String> exemplos = new ArrayList<>();
        final List<String> recusas = new ArrayList<>();
        int itens = 0;
        int notasComViolacao = 0;

        final Path diretorio = saida != null ? Files.createDirectories(Path.of(saida)) : null;
        try (BufferedWriter registro = diretorio != null ? abrirRegistro(diretorio.resolve("lote.jsonl.gz")) : null;
                BufferedWriter detalhe = diretorio != null ? abrirRegistro(diretorio.resolve("violacoes-detalhe.txt.gz")) : null) {
            for (int i = 0; i < quantidade; i++) {
                final Nota nota = GeradorLoteArredondamento.gerarNota(semente, i);
                final var resposta = mockMvc.perform(post("/calculadora/regime-geral")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nota.corpo()))
                        .andReturn().getResponse();
                final int status = resposta.getStatus();
                final String corpo = resposta.getContentAsString(StandardCharsets.UTF_8);
                if (registro != null) {
                    registro.write("{\"indice\":" + nota.indice() + ",\"perfil\":\"" + nota.perfil() + "\",\"pedido\":"
                            + nota.corpo() + ",\"status\":" + status + ",\"resposta\":" + corpo + "}\n");
                }
                if (status != 200) {
                    recusas.add("nota " + nota.indice() + " (" + nota.perfil() + "): HTTP " + status + " " + corpo);
                    continue;
                }
                final JsonNode json = objectMapper.readTree(corpo);
                itens += json.path("objetos").size();
                final List<Violacao> violacoes = VerificadorInvariantesArredondamento.verificar(json);
                if (!violacoes.isEmpty()) {
                    notasComViolacao++;
                    final var invariantesDaNota = new java.util.TreeSet<String>();
                    for (Violacao v : violacoes) {
                        violacoesPorInvariante.merge(v.invariante(), 1, Integer::sum);
                        invariantesDaNota.add(v.invariante());
                        if (detalhe != null) {
                            detalhe.write("nota " + nota.indice() + " (" + nota.perfil() + ") " + v + "\n");
                        }
                        if (exemplos.size() < MAX_VIOLACOES_EXIBIDAS) {
                            exemplos.add("nota " + nota.indice() + " (" + nota.perfil() + ") " + v);
                        }
                    }
                    invariantesDaNota.forEach(inv -> notasPorInvariante.merge(inv, 1, Integer::sum));
                }
            }
        }

        final StringBuilder resumo = new StringBuilder();
        resumo.append("semente: ").append(semente).append('\n');
        resumo.append("notas: ").append(quantidade).append('\n');
        resumo.append("itens calculados: ").append(itens).append('\n');
        resumo.append("notas recusadas (HTTP diferente de 200): ").append(recusas.size()).append('\n');
        resumo.append("notas com violação: ").append(notasComViolacao).append('\n');
        for (String inv : List.of("I1", "I2", "I3", "I4", "I5", "I6", "I7")) {
            resumo.append(inv).append(": ").append(violacoesPorInvariante.getOrDefault(inv, 0)).append(" violações em ")
                    .append(notasPorInvariante.getOrDefault(inv, 0)).append(" notas\n");
        }
        if (diretorio != null) {
            Files.writeString(diretorio.resolve("violacoes.txt"),
                    resumo + "\nPrimeiras violações:\n" + String.join("\n", exemplos) + "\n\nRecusas:\n"
                            + String.join("\n", recusas.subList(0, Math.min(recusas.size(), MAX_VIOLACOES_EXIBIDAS))) + "\n",
                    StandardCharsets.UTF_8);
        }

        assertThat(recusas).as("Notas recusadas no lote%n%s", resumo).isEmpty();
        assertThat(exemplos).as("Violações de invariante no lote%n%s", resumo).isEmpty();
    }

    private static BufferedWriter abrirRegistro(Path arquivo) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(arquivo)), StandardCharsets.UTF_8));
    }
}
