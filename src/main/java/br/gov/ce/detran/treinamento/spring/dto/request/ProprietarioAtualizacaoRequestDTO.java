package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.util.validation.CpfCnpj;
import br.gov.ce.detran.treinamento.spring.util.Documento;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos ausentes ou nulos mantêm os valores atuais do proprietário. Use com @Valid. */
public record ProprietarioAtualizacaoRequestDTO(
        @CpfCnpj String cpfCnpj,
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.") String nome,
        @Size(max = 150, message = "Rua deve ter no máximo 150 caracteres.") String rua,
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.") String numero,
        @Pattern(regexp = "[0-9]{8}", message = "CEP deve conter 8 dígitos.") String cep,
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.") String cidade,
        @Pattern(regexp = "[A-Z]{2}", message = "Estado deve conter a UF com 2 letras.") String estado,
        @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres.") String complemento) {

    public ProprietarioAtualizacaoRequestDTO {
        cpfCnpj = cpfCnpj == null ? null : Normalizacao.campoAtualizacao(Documento.limparCpfCnpj(cpfCnpj));
        nome = Normalizacao.campoAtualizacao(nome);
        rua = Normalizacao.campoAtualizacao(rua);
        numero = Normalizacao.campoAtualizacao(numero);
        cep = Normalizacao.cep(Normalizacao.campoAtualizacao(cep));
        cidade = Normalizacao.campoAtualizacao(cidade);
        estado = Normalizacao.campoAtualizacao(estado);
        complemento = Normalizacao.campoAtualizacao(complemento);
    }

}
