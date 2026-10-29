package br.gov.ce.detran.treinamento.spring.dto.response;

import br.gov.ce.detran.treinamento.spring.model.Veiculo;
import java.time.OffsetDateTime;

public record VeiculoResponseDTO(
        Integer id,
        String tipo,
        Integer idProprietario,
        Integer anoFabricacao,
        String chassi,
        String renavam,
        String placa,
        OffsetDateTime excluidoEm) {

    public static VeiculoResponseDTO fromEntity(Veiculo veiculo) {
        return new VeiculoResponseDTO(
                veiculo.getId(),
                veiculo.getTipo() == null ? null : veiculo.getTipo().toString(),
                veiculo.getIdProprietario() == null ? null : veiculo.getIdProprietario().getId(),
                veiculo.getAnoFabricacao(),
                veiculo.getChassi(),
                veiculo.getRenavam(),
                veiculo.getPlaca(),
                veiculo.getExcluidoEm());
    }
}
