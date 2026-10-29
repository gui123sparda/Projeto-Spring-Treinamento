package br.gov.ce.detran.treinamento.spring.globalExceptions;

import br.gov.ce.detran.treinamento.spring.globalExceptions.apiException.ApiException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.NegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalException extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalException.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> tratarNegocio(NegocioException exception, WebRequest request) {
        return responder(exception, exception.getStatus(), exception.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var erros = exception.getBindingResult().getAllErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro instanceof FieldError campo ? campo.getField() : erro.getObjectName(),
                        "mensagem", erro.getDefaultMessage() == null ? "Valor inválido." : erro.getDefaultMessage()))
                .toList();
        var resposta = ApiException.criar(status.value(), exception,
                "Existem campos inválidos na requisição.", erros);
        return handleExceptionInternal(exception, resposta, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var resposta = ApiException.criar(status.value(), exception,
                "Corpo da requisição ausente ou inválido. Confira o JSON e os tipos dos campos.", List.of());
        return handleExceptionInternal(exception, resposta, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> tratarValidacao(ConstraintViolationException exception, WebRequest request) {
        var erros = exception.getConstraintViolations().stream()
                .map(erro -> Map.of("campo", erro.getPropertyPath().toString(), "mensagem", erro.getMessage()))
                .toList();
        var resposta = ApiException.criar(400, exception, "Existem campos inválidos na requisição.", erros);
        return handleExceptionInternal(exception, resposta, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> tratarArgumentoInvalido(IllegalArgumentException exception, WebRequest request) {
        return responder(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(DataIntegrityViolationException exception, WebRequest request) {
        return responder(exception, HttpStatus.CONFLICT,
                "Operação viola uma restrição de integridade dos dados.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarErroInesperado(Exception exception, WebRequest request) {
        LOG.error("Erro inesperado ao processar a requisição", exception);
        return responder(exception, HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno. Tente novamente mais tarde.", request);
    }

    private ResponseEntity<Object> responder(Exception exception, HttpStatusCode status,
                                             String mensagem, WebRequest request) {
        var resposta = ApiException.criar(status.value(), exception, mensagem, List.of());
        return handleExceptionInternal(exception, resposta, new HttpHeaders(), status, request);
    }

    /** Aplica o mesmo corpo aos erros HTTP tratados pelo próprio Spring (404, 405, etc.). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (!(body instanceof ApiException)) {
            if (body == null && exception instanceof ErrorResponse erroHttp) {
                body = erroHttp.getBody();
            }
            var mensagem = body instanceof ProblemDetail problema && problema.getDetail() != null
                    ? problema.getDetail() : "Não foi possível processar a requisição.";
            body = ApiException.criar(status.value(), exception, mensagem, List.of());
        }
        var cabecalhos = new HttpHeaders();
        cabecalhos.putAll(headers);
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(exception, body, cabecalhos, status, request);
    }
}
