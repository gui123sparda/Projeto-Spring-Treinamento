package br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions;

public class VeiculoNaoEncontradoException extends RecursoNaoEncontradoException {
    public VeiculoNaoEncontradoException() {
        super("Veículo não encontrado.");
    }
}
