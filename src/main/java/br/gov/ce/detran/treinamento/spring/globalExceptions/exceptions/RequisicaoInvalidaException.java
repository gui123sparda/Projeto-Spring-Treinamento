package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

import org.springframework.http.HttpStatus;

public class RequisicaoInvalidaException extends NegocioException {
    public RequisicaoInvalidaException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, mensagem);
    }
}
