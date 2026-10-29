package br.gov.ce.detran.treinamento.spring.dto.response;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;

/** Dados de um proprietário para retorno da API. */
public record ProprietarioResponseDTO(
        Integer id,
        String cpfCnpj,
        String nome,
        String rua,
        String numero,
        String cep,
        String cidade,
        String estado,
        String complemento) {

    public static ProprietarioResponseDTO fromEntity(Proprietario proprietario) {
        return new ProprietarioResponseDTO(
                proprietario.getId(),
                proprietario.getCpfCnpj(),
                proprietario.getNome(),
                proprietario.getRua(),
                proprietario.getNumero(),
                proprietario.getCep(),
                proprietario.getCidade(),
                proprietario.getEstado(),
                proprietario.getComplemento());
    }
}
