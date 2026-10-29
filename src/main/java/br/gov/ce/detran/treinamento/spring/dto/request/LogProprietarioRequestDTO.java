package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.model.LogProprietario;
import br.gov.ce.detran.treinamento.spring.util.validation.CpfCnpj;
import br.gov.ce.detran.treinamento.spring.util.Documento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/** Dados para criação interna de log do proprietário. */
public record LogProprietarioRequestDTO(
        @NotNull(message = "Informe o proprietário.")
        @Positive(message = "ID do proprietário deve ser positivo.") Integer idProprietario,
        @NotBlank(message = "Informe o CPF/CNPJ.") @CpfCnpj String cpfCnpj,
        @NotNull(message = "Informe a data de criação.") Instant dataCriacao,
        Instant dataAlteracao) {

    public LogProprietarioRequestDTO {
        cpfCnpj = Documento.limparCpfCnpj(cpfCnpj);
    }

    public LogProprietario toEntity() {
        var log = new LogProprietario();
        log.setIdProprietario(idProprietario);
        log.setCpfCnpj(cpfCnpj);
        log.setDataCriacao(dataCriacao);
        log.setDataAlteracao(dataAlteracao);
        return log;
    }
}
