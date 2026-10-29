package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

import org.springframework.http.HttpStatus;

/** Classe base das exceções de negócio com mensagem destinada ao cliente. */
public abstract class NegocioException extends RuntimeException {
    private final HttpStatus status;

    protected NegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
