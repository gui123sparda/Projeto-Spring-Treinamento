package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

public class LogProprietarioNaoEncontradoException extends RecursoNaoEncontradoException {
    public LogProprietarioNaoEncontradoException() {
        super("Log do proprietário não encontrado.");
    }
}
