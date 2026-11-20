/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.shared.exceptionhandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import br.gov.serpro.rtc.shared.exceptionhandler.enumeration.ProblemType;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler ÚNICO de exceções da API ({@code @RestControllerAdvice} exclusivo do
 * projeto), que traduz erros de validação, erros de cálculo e falhas internas
 * para respostas {@link ProblemDetail} com estrutura unificada: {@code type},
 * {@code title}, {@code timestamp} e, quando há erros de campo/parâmetro, a
 * propriedade {@code errors[]} com objetos {@link ErroDetalhe} cujo
 * {@code pointer} segue a RFC 6901.
 *
 * <p>Por ser o único advice, o handler mais específico vence o catch-all de
 * {@link Exception} dentro do próprio advice — não há necessidade de
 * {@code @Order} (o antigo {@code SimplesNacionalExceptionHandler}, hoje
 * absorvido aqui, precisava de precedência máxima apenas por coexistir com
 * este advice).
 *
 * <p>Também ajusta o nível de log conforme a natureza do erro (5xx com
 * stacktrace em {@code ERROR}; 4xx em {@code DEBUG}) e padroniza o payload
 * retornado aos clientes.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI TYPE_PADRAO = URI.create("about:blank");

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {

        // Estratégia simples: apenas erros 500+ são logados como ERROR (erros reais)
        // Erros 400-499 são comportamento esperado (erros de negócio/validação)
        if (statusCode.is5xxServerError()) {
            // Erro real do sistema - log completo para análise
            log.error("Erro interno: ", ex);
        } else {
            // Erro de negócio/validação - log simples sem stacktrace
            log.debug("Erro cliente [{}]: {} | URI: {}",
                     statusCode.value(),
                     ex.getMessage(),
                     request.getDescription(false));
        }

        ResponseEntity<Object> resposta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        // O Spring Framework 7 deixou de preencher o "type" padrão (RFC 9457: ausente equivale a
        // "about:blank"). Preenchido aqui para o corpo dos erros continuar com o campo.
        if (resposta != null && resposta.getBody() instanceof ProblemDetail problema && problema.getType() == null) {
            problema.setType(TYPE_PADRAO);
        }
        return resposta;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = createProblem(ex, httpStatus);
        problemDetail.setDetail(ex.getLocalizedMessage());
        return handleExceptionInternal(ex, problemDetail, headers, httpStatus, request);
    }

    /**
     * Query param com valor mal-formado (ex.: {@code data=15/03/2027} nos
     * endpoints de atividades): HTTP 400 com {@code errors[]} apontando o
     * parâmetro ofensor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String detail = String.format("Parâmetro inválido [%s: %s] ", ex.getPropertyName(), ex.getValue());
        ProblemDetail problemDetail = createProblem(ex, status);
        problemDetail.setDetail(detail);
        problemDetail.setProperty("errors", List.of(
                erroSemCatalogo("Tipo de argumento inválido", ex.getPropertyName(), detail.trim())));
        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

    /**
     * Query param obrigatório ausente (ex.: {@code data} nos endpoints de
     * atividades): HTTP 400 padronizado com {@code errors[]} apontando o
     * parâmetro, em vez do tratamento padrão do Spring.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        String detail = String.format("O parâmetro obrigatório '%s' (%s) não foi informado.",
                ex.getParameterName(), ex.getParameterType());
        ProblemDetail problemDetail = createProblem(ex, httpStatus);
        problemDetail.setDetail(detail);
        problemDetail.setProperty("errors", List.of(
                erroSemCatalogo("Parâmetro obrigatório ausente", ex.getParameterName(), detail)));
        return handleExceptionInternal(ex, problemDetail, headers, httpStatus, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        // Log sucinto para validação - sem stacktrace
        log.debug("Validação falhou: {} campos inválidos | URI: {}",
                 ex.getBindingResult().getErrorCount(),
                 request.getDescription(false));

        ProblemDetail problemDetail = createProblem(ex, httpStatus);
        problemDetail.setTitle("Campos inválidos.");
        StringBuilder detailMessage = new StringBuilder("Um ou mais campos são inválidos - ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
            detailMessage.append(String.format("[%s: %s] ", error.getField(), error.getDefaultMessage()))
        );
        problemDetail.setDetail(detailMessage.toString());
        problemDetail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> erroSemCatalogo("Campo inválido", error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(ex, problemDetail, headers, httpStatus, request);
    }

    /**
     * Erros globais de cálculo em modo de observabilidade
     * ({@link ErrosCalculoException}): HTTP 422 com a lista detalhada de erros
     * na propriedade {@code errors} (absorvido do antigo
     * {@code SimplesNacionalExceptionHandler}; os {@link ErroDetalhe} chegam
     * prontos, com os códigos do catálogo da feature — ex.: {@code VSN-}).
     */
    @ExceptionHandler(ErrosCalculoException.class)
    ResponseEntity<ProblemDetail> handleErrosCalculoException(ErrosCalculoException ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        List<ErroDetalhe> erros = ex.getErros();

        log.debug("Erros de observabilidade: {} erro(s) | URI: {}",
                erros.size(), request.getDescription(false));

        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setType(java.net.URI.create("about:blank"));
        problemDetail.setTitle("Erros no processamento do cálculo do Simples Nacional");
        problemDetail.setDetail(String.format("O cálculo contém %d erro(s).", erros.size()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", erros);
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = createProblem(ex, status);
        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

    private static <E extends Exception> ProblemDetail createProblem(E ex, HttpStatus status) {
        ProblemType type = ProblemType.from(ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setType(type.getURI());
        problemDetail.setTitle(type.getTitulo());
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    /**
     * Monta um {@link ErroDetalhe} genérico (sem catálogo próprio): {@code type}
     * e {@code code} nulos são omitidos pela inclusão {@code NON_NULL} do record;
     * o {@code pointer} segue a RFC 6901 ({@code "#/" + campo}).
     */
    private static ErroDetalhe erroSemCatalogo(String title, String campo, String detail) {
        return new ErroDetalhe(null, title, null, "#/" + campo, detail);
    }

}
