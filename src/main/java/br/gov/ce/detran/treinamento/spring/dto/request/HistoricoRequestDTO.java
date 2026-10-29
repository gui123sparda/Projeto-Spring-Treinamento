package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.model.Historico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Dados para criação interna de histórico; não implica um endpoint público de auditoria.
 */
public record HistoricoRequestDTO(

		@NotBlank (message = "Informe a entidade.")
		String entidade,

		@NotNull (message = "Informe o ID do registro.")
		@Positive (message = "ID do registro deve ser positivo.")
		Long registroId,

		@NotBlank (message = "Informe a operação.")
		String operacao,

		@NotNull (message = "Informe a data da operação.")
		OffsetDateTime ocorridoEm,

		@NotBlank (message = "Informe o usuário do banco.")
		String usuarioBanco,

		Map<String, Object> antes,
		Map<String, Object> depois) {

	public Historico toEntity() {
		var historico = new Historico();
		historico.setEntidade(entidade);
		historico.setRegistroId(registroId);
		historico.setOperacao(operacao);
		historico.setOcorridoEm(ocorridoEm);
		historico.setUsuarioBanco(usuarioBanco);
		historico.setAntes(antes);
		historico.setDepois(depois);
		return historico;
	}
}
