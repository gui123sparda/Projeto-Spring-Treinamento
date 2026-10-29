package br.gov.ce.detran.treinamento.spring.dto.response;

import br.gov.ce.detran.treinamento.spring.model.Historico;
import java.time.OffsetDateTime;
import java.util.Map;

public record HistoricoResponseDTO(
        Long id,
        String entidade,
        Long registroId,
        String operacao,
        OffsetDateTime ocorridoEm,
        String usuarioBanco,
        Map<String, Object> antes,
        Map<String, Object> depois) {

    public static HistoricoResponseDTO fromEntity(Historico historico) {
        return new HistoricoResponseDTO(
                historico.getId(),
                historico.getEntidade(),
                historico.getRegistroId(),
                historico.getOperacao(),
                historico.getOcorridoEm(),
                historico.getUsuarioBanco(),
                historico.getAntes(),
                historico.getDepois());
    }
}
