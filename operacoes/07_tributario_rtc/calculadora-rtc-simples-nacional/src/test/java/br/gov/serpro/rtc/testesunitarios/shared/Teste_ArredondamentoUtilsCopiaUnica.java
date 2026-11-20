package br.gov.serpro.rtc.testesunitarios.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * A classe {@code ArredondamentoUtils} e o teste {@code Teste_ArredondamentoUtils} são cópias
 * idênticas, byte a byte, na {@code api-regime-geral} e na {@code api-simples-nacional}. Este teste
 * confere o SHA-256 dos dois arquivos contra constantes que são as mesmas nos dois projetos.
 * <p>
 * Quem alterar a classe (ou o teste dela) altera a cópia do outro projeto e atualiza a constante
 * correspondente aqui, também nos dois projetos. O valor esperado é o que {@code sha256sum}
 * imprime para o arquivo com fim de linha LF; o teste converte CRLF em LF antes de calcular, para
 * não depender da configuração de fim de linha do git em cada máquina.
 */
class Teste_ArredondamentoUtilsCopiaUnica {

    private static final String SHA256_CLASSE = "bdb9540b3085acf64656c734763a3a9e363a42f9b51e33a69c6407d26dad0495";

    private static final String SHA256_TESTE = "41abe255f9f0ecd6eae08313589cd7010be5761cfab059cece5f780cb243bb7e";

    @Test
    void classe_eACopiaUnicaDosDoisProjetos() throws Exception {
        assertEquals(SHA256_CLASSE,
                sha256(Path.of("src/main/java/br/gov/serpro/rtc/shared/util/ArredondamentoUtils.java")),
                "ArredondamentoUtils.java mudou: replique a alteração na cópia do outro projeto e atualize "
                        + "SHA256_CLASSE neste teste, nos dois projetos");
    }

    @Test
    void testeDaClasse_eACopiaUnicaDosDoisProjetos() throws Exception {
        assertEquals(SHA256_TESTE,
                sha256(Path.of("src/test/java/br/gov/serpro/rtc/testesunitarios/shared/Teste_ArredondamentoUtils.java")),
                "Teste_ArredondamentoUtils.java mudou: replique a alteração na cópia do outro projeto e "
                        + "atualize SHA256_TESTE neste teste, nos dois projetos");
    }

    private static String sha256(Path arquivo) throws Exception {
        final String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8).replace("\r\n", "\n");
        final byte[] hash = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
