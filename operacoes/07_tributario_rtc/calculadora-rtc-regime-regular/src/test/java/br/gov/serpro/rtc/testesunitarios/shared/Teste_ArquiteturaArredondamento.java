package br.gov.serpro.rtc.testesunitarios.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Teste de arquitetura da política numérica: no código de produção, só a classe
 * {@code ArredondamentoUtils} arredonda. Fora dela não pode haver {@code setScale(},
 * {@code RoundingMode} nem {@code MathContext}; e um {@code setRoundingMode(} só é aceito com o
 * modo da classe ({@code MODO}), nunca com modo próprio.
 * <p>
 * Comentários não contam: o texto é conferido depois de removidos os comentários de bloco e de
 * linha. Este teste é idêntico na {@code api-regime-geral} e na {@code api-simples-nacional}.
 */
class Teste_ArquiteturaArredondamento {

    private static final Path RAIZ = Path.of("src/main/java");

    private static final String CLASSE_DA_POLITICA = "ArredondamentoUtils.java";

    private static final Pattern COMENTARIOS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);

    private static final List<Pattern> PROIBIDOS = List.of(
            Pattern.compile("\\bsetScale\\s*\\("),
            Pattern.compile("\\bRoundingMode\\b"),
            Pattern.compile("\\bMathContext\\b"),
            // setRoundingMode com modo que não seja o MODO da classe
            Pattern.compile("\\bsetRoundingMode\\s*\\(\\s*(?!(ArredondamentoUtils\\.)?MODO\\s*\\))"));

    @Test
    void soAClasseDaPoliticaArredonda() throws IOException {
        final List<String> ocorrencias = new ArrayList<>();
        final List<Path> arquivos;
        try (Stream<Path> caminhos = Files.walk(RAIZ)) {
            arquivos = caminhos.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        assertTrue(arquivos.size() > 10, "arquivos de produção encontrados em " + RAIZ.toAbsolutePath());
        assertTrue(arquivos.stream().anyMatch(p -> p.getFileName().toString().equals(CLASSE_DA_POLITICA)),
                "a classe da política numérica existe em " + RAIZ);

        for (Path arquivo : arquivos) {
            if (arquivo.getFileName().toString().equals(CLASSE_DA_POLITICA)) {
                continue;
            }
            final String codigo = semComentarios(Files.readString(arquivo, StandardCharsets.UTF_8));
            for (Pattern proibido : PROIBIDOS) {
                final Matcher m = proibido.matcher(codigo);
                while (m.find()) {
                    ocorrencias.add(RAIZ.relativize(arquivo) + ": " + trecho(codigo, m.start()));
                }
            }
        }
        assertEquals(List.of(), ocorrencias,
                "Arredondamento fora de ArredondamentoUtils: use os métodos da classe (e, se faltar algum, "
                        + "acrescente-o nas duas cópias da classe)");
    }

    @Test
    void asRegrasPegamOQueDevemPegar() {
        final String proibido = """
                valor.setScale(2, RoundingMode.HALF_UP);
                valor.divide(outro, MathContext.DECIMAL128);
                formato.setRoundingMode(java.math.RoundingMode.DOWN);
                formato.setRoundingMode(modoProprio);
                """;
        long achados = 0;
        for (Pattern p : PROIBIDOS) {
            achados += p.matcher(proibido).results().count();
        }
        // setScale, 2 × RoundingMode, MathContext e 2 × setRoundingMode com modo próprio
        assertEquals(6, achados);

        final String permitido = semComentarios("""
                // valor.setScale(2, RoundingMode.HALF_UP) num comentário de linha
                /* MathContext num comentário de bloco */
                formato.setRoundingMode(MODO);
                formato.setRoundingMode(ArredondamentoUtils.MODO);
                ArredondamentoUtils.arredondarValorFiscal(valor);
                """);
        for (Pattern p : PROIBIDOS) {
            assertEquals(0, p.matcher(permitido).results().count(), p.pattern());
        }
    }

    private static String semComentarios(String codigo) {
        return COMENTARIOS.matcher(codigo).replaceAll("");
    }

    private static String trecho(String codigo, int posicao) {
        final int inicio = codigo.lastIndexOf('\n', posicao) + 1;
        int fim = codigo.indexOf('\n', posicao);
        if (fim < 0) {
            fim = codigo.length();
        }
        return codigo.substring(inicio, fim).trim();
    }
}
