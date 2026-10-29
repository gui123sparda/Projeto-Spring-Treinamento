package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import br.gov.ce.detran.treinamento.spring.model.Veiculo;
import br.gov.ce.detran.treinamento.spring.model.TipoVeiculo;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public record VeiculoRequestDTO(
        @NotBlank(message = "Informe o tipo do veículo.")
        @jakarta.validation.constraints.Pattern(regexp = "CARRO|MOTO", message = "Tipo deve ser CARRO ou MOTO.") String tipo,
        @NotNull(message = "Informe o proprietário.")
        @Positive(message = "ID do proprietário deve ser positivo.") Integer idProprietario,
        @NotNull(message = "Informe o ano de fabricação.")
        @Positive(message = "Ano de fabricação deve ser positivo.") Integer anoFabricacao,
        @NotBlank(message = "Informe o chassi.")
        @Size(max = 17, message = "Chassi deve ter no máximo 17 caracteres.") String chassi,
        @NotBlank(message = "Informe o Renavam.")
        @Size(max = 11, message = "Renavam deve ter no máximo 11 caracteres.") String renavam,
        @NotBlank(message = "Informe a placa.")
        @Size(max = 7, message = "Placa deve ter no máximo 7 caracteres.") String placa) {

    public VeiculoRequestDTO {
        tipo = Normalizacao.texto(tipo);
        chassi = Normalizacao.texto(chassi);
        renavam = Normalizacao.texto(renavam);
        placa = Normalizacao.texto(placa);
    }

    /** Recebe o proprietário consultado pelo service usando idProprietario. */
    public Veiculo toEntity(Proprietario proprietario) {
        Objects.requireNonNull(proprietario, "Informe o proprietário consultado.");
        if (idProprietario == null || !idProprietario.equals(proprietario.getId())) {
            throw new IllegalArgumentException("O proprietário deve corresponder ao ID informado.");
        }
        var veiculo = new Veiculo();
        veiculo.setTipo(TipoVeiculo.valueOf(tipo));
        veiculo.setIdProprietario(proprietario);
        veiculo.setAnoFabricacao(anoFabricacao);
        veiculo.setChassi(chassi);
        veiculo.setRenavam(renavam);
        veiculo.setPlaca(placa);
        return veiculo;
    }
}
