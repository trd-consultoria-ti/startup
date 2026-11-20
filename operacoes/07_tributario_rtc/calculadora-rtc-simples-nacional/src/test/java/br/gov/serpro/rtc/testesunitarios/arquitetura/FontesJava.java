package br.gov.serpro.rtc.testesunitarios.arquitetura;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Leitura das fontes de {@code src/main/java} e {@code src/test/java} para os
 * testes de arquitetura, sem dependência nova: procura referências a pacotes
 * fora de comentários.
 */
public final class FontesJava {

    private static final String RAIZ_PACOTES = "br/gov/serpro/rtc";

    private static final Pattern COMENTARIO_BLOCO = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern COMENTARIO_LINHA = Pattern.compile("//[^\\n]*");

    private static final List<Path> RAIZES = List.of(Path.of("src/main/java"), Path.of("src/test/java"));

    private FontesJava() {
    }

    /** Todas as fontes Java de main e test; falha se a pasta de fontes principal não for encontrada. */
    public static Stream<Path> fontes() {
        assertTrue(Files.isDirectory(RAIZES.get(0)), "fontes não encontradas em " + RAIZES.get(0).toAbsolutePath());
        return RAIZES.stream()
                .filter(Files::isDirectory)
                .flatMap(FontesJava::listar)
                .filter(fonte -> fonte.toString().endsWith(".java"));
    }

    /** A fonte está no pacote informado (relativo a {@code br/gov/serpro/rtc}) ou num subpacote dele. */
    public static boolean estaNoPacote(Path fonte, String pacote) {
        return fonte.toString().replace('\\', '/').contains("/" + RAIZ_PACOTES + "/" + pacote + "/");
    }

    /** O código da fonte (sem comentários) menciona o pacote, num import ou num nome qualificado. */
    public static boolean referencia(Path fonte, String pacote) {
        String codigo = COMENTARIO_LINHA.matcher(COMENTARIO_BLOCO.matcher(ler(fonte)).replaceAll(" ")).replaceAll(" ");
        return codigo.contains(pacote + ".") || codigo.contains(pacote + ";");
    }

    private static Stream<Path> listar(Path raiz) {
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            return arquivos.toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String ler(Path fonte) {
        try {
            return Files.readString(fonte, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
