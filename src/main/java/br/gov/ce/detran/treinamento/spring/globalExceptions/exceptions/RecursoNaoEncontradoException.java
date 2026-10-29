package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

import org.springframework.http.HttpStatus;

public class RecursoNaoEncontradoException extends NegocioException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }
}
