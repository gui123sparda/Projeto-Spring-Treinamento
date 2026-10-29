package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Campos ausentes ou nulos mantêm os valores atuais do veículo. */
public record VeiculoAtualizacaoRequestDTO(
        @jakarta.validation.constraints.Pattern(regexp = "CARRO|MOTO", message = "Tipo deve ser CARRO ou MOTO.") String tipo,
        @Positive(message = "ID do proprietário deve ser positivo.") Integer idProprietario,
        @Positive(message = "Ano de fabricação deve ser positivo.") Integer anoFabricacao,
        @Size(max = 17, message = "Chassi deve ter no máximo 17 caracteres.") String chassi,
        @Size(max = 11, message = "Renavam deve ter no máximo 11 caracteres.") String renavam,
        @Size(max = 7, message = "Placa deve ter no máximo 7 caracteres.") String placa) {

    public VeiculoAtualizacaoRequestDTO {
        tipo = Normalizacao.campoAtualizacao(tipo);
        chassi = Normalizacao.campoAtualizacao(chassi);
        renavam = Normalizacao.campoAtualizacao(renavam);
        placa = Normalizacao.campoAtualizacao(placa);
    }

}
