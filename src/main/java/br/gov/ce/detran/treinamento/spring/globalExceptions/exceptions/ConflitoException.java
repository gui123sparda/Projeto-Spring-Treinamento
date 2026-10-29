package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

import org.springframework.http.HttpStatus;

public class ConflitoException extends NegocioException {
    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
