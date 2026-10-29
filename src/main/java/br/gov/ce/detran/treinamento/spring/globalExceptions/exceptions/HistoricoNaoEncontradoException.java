package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

public class HistoricoNaoEncontradoException extends RecursoNaoEncontradoException {
    public HistoricoNaoEncontradoException() {
        super("Histórico não encontrado.");
    }
}
