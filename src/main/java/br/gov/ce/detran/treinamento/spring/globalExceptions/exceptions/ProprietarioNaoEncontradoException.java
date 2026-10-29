package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

public class ProprietarioNaoEncontradoException extends RecursoNaoEncontradoException {
    public ProprietarioNaoEncontradoException() {
        super("Proprietário não encontrado.");
    }
}
