/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.shared.exceptionhandler.enumeration;

import java.net.URI;
import java.util.stream.Stream;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.gov.serpro.rtc.shared.util.HttpUtils;
import lombok.Getter;
import lombok.NonNull;

/**
 * Catálogo dos tipos de erro genéricos expostos pela API, relacionando
 * exceções a títulos e paths usados no {@code ProblemDetail}.
 */
@Getter
public enum ProblemType {

    MENSAGEM_INCOMPREENSIVEL(HttpMessageNotReadableException.class,
            "Mensagem incompreensível", "mensagem-incompreensivel"),

    METHOD_ARGUMENT_TYPE_MISMATCH(MethodArgumentTypeMismatchException.class,
            "Tipo de argumento inválido", "tipo-argumento-invalido"),

    PARAMETRO_OBRIGATORIO_AUSENTE(MissingServletRequestParameterException.class,
            "Parâmetro obrigatório ausente", "parametro-obrigatorio-ausente"),

    CAMPOS_INVALIDOS(MethodArgumentNotValidException.class,
            "Campos inválidos.", "campos-invalidos"),

    ERRO_SISTEMA(Exception.class,
            "Erro de sistema não previsto", "erro-de-sistema-nao-previsto");

    private final Class<? extends Exception> classeErro;
    private final String titulo;
    private final String path;

    private ProblemType(Class<? extends Exception> classeErro, String titulo, String path) {
        this.classeErro = classeErro;
        this.titulo = titulo;
        this.path = path;
    }

    public URI getURI() {
        return URI.create(String.format("%s/errors/%s", HttpUtils.getBaseURL(), path));
    }

    public static <E extends Exception> ProblemType from(@NonNull final E e) {
        Class<? extends Exception> classeErro = e.getClass();
        return Stream.of(values()).filter(t -> t.getClasseErro().equals(classeErro)).findFirst().orElse(ERRO_SISTEMA);
    }

}
