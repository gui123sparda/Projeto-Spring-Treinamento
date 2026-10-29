package br.gov.ce.detran.treinamento.spring.dto.response;

import br.gov.ce.detran.treinamento.spring.model.LogProprietario;
import java.time.Instant;

public record LogProprietarioResponseDTO(
        Integer id,
        Integer idProprietario,
        String cpfCnpj,
        Instant dataCriacao,
        Instant dataAlteracao) {

    public static LogProprietarioResponseDTO fromEntity(LogProprietario log) {
        return new LogProprietarioResponseDTO(
                log.getId(),
                log.getIdProprietario(),
                log.getCpfCnpj(),
                log.getDataCriacao(),
                log.getDataAlteracao());
    }
}
