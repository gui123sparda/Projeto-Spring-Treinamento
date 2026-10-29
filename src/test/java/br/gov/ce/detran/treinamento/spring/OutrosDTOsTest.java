package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.HistoricoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.LogProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoRequestDTO;
import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OutrosDTOsTest {
    @Test
    void impedeVincularVeiculoAOutroProprietario() {
        var proprietario = new Proprietario();
        proprietario.setId(2);
        assertThatIllegalArgumentException().isThrownBy(() -> veiculo().toEntity(proprietario));
    }

    @Test
    void vinculaVeiculoAoProprietarioConsultado() {
        var proprietario = new Proprietario();
        proprietario.setId(1);
        assertThat(veiculo().toEntity(proprietario).getIdProprietario()).isSameAs(proprietario);
    }

    @Test
    void requestsRejeitamCamposObrigatoriosAusentes() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new VeiculoRequestDTO(null, null, null, null, null, null)))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("tipo", "idProprietario", "anoFabricacao", "chassi", "renavam", "placa");
            assertThat(validator.validate(new HistoricoRequestDTO(null, null, null, null, null, null, null)))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("entidade", "registroId", "operacao", "ocorridoEm", "usuarioBanco");
            assertThat(validator.validate(new LogProprietarioRequestDTO(null, null, null, null)))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("idProprietario", "cpfCnpj", "dataCriacao");
        }
    }

    private static VeiculoRequestDTO veiculo() {
        return new VeiculoRequestDTO("CARRO", 1, 2020, "9BWZZZPZ5FP123456", "12345678901", "ABC1D23");
    }
}
