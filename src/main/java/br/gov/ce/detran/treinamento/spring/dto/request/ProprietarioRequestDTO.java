package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import br.gov.ce.detran.treinamento.spring.util.validation.CpfCnpj;
import br.gov.ce.detran.treinamento.spring.util.Documento;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados de proprietário. Use @Valid no parâmetro do controller para validar a entrada. */
public record ProprietarioRequestDTO(
        @NotBlank(message = "Informe o CPF/CNPJ.") @CpfCnpj String cpfCnpj,
        @NotBlank(message = "Informe o nome.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.") String nome,
        @Size(max = 150, message = "Rua deve ter no máximo 150 caracteres.") String rua,
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.") String numero,
        @Pattern(regexp = "[0-9]{8}", message = "CEP deve conter 8 dígitos.") String cep,
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.") String cidade,
        @Pattern(regexp = "[A-Z]{2}", message = "Estado deve conter a UF com 2 letras.") String estado,
        @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres.") String complemento) {

    public ProprietarioRequestDTO {
        cpfCnpj = Documento.limparCpfCnpj(cpfCnpj);
        nome = Normalizacao.texto(nome);
        rua = Normalizacao.texto(rua);
        numero = Normalizacao.texto(numero);
        cep = Normalizacao.cep(cep);
        cidade = Normalizacao.texto(cidade);
        estado = Normalizacao.texto(estado);
        complemento = Normalizacao.texto(complemento);
    }

    /** Cria uma entidade para cadastro; o ID é gerado pelo banco. */
    public Proprietario toEntity() {
        var proprietario = new Proprietario();
        proprietario.setCpfCnpj(cpfCnpj);
        proprietario.setNome(nome);
        proprietario.setRua(rua);
        proprietario.setNumero(numero);
        proprietario.setCep(cep);
        proprietario.setCidade(cidade);
        proprietario.setEstado(estado);
        proprietario.setComplemento(complemento);
        return proprietario;
    }
}
