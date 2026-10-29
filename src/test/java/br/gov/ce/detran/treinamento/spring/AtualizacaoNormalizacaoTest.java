package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoAtualizacaoRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AtualizacaoNormalizacaoTest {
    @Test
    void normalizaDadosInformadosNosDoisTiposDeAtualizacao() {
        var proprietario = new ProprietarioAtualizacaoRequestDTO("12.abc.345/01de-35", " João ",
                " Rua A ", " 10a ", "60.000-000", " Fortaleza ", " ce ", " casa ");
        assertThat(proprietario.cpfCnpj()).isEqualTo("12ABC34501DE35");
        assertThat(proprietario.nome()).isEqualTo("JOÃO");
        assertThat(proprietario.rua()).isEqualTo("RUA A");
        assertThat(proprietario.numero()).isEqualTo("10A");
        assertThat(proprietario.cep()).isEqualTo("60000000");
        assertThat(proprietario.cidade()).isEqualTo("FORTALEZA");
        assertThat(proprietario.estado()).isEqualTo("CE");
        assertThat(proprietario.complemento()).isEqualTo("CASA");

        var veiculo = new VeiculoAtualizacaoRequestDTO(" carro ", 1, 2020,
                " abc123 ", " 12345678901 ", " abc1d23 ");
        assertThat(veiculo.tipo()).isEqualTo("CARRO");
        assertThat(veiculo.chassi()).isEqualTo("ABC123");
        assertThat(veiculo.renavam()).isEqualTo("12345678901");
        assertThat(veiculo.placa()).isEqualTo("ABC1D23");
    }

    @Test
    void preservaAusenciaDeCamposNasAtualizacoes() {
        var proprietario = new ProprietarioAtualizacaoRequestDTO(null, null, null, null, null, null, null, null);
        assertThat(proprietario).hasAllNullFieldsOrProperties();
        var veiculo = new VeiculoAtualizacaoRequestDTO(null, null, null, null, null, null);
        assertThat(veiculo).hasAllNullFieldsOrProperties();
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejeitaCamposExplicitamenteEmBrancoNosDoisTiposDeAtualizacao(String valor) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ProprietarioAtualizacaoRequestDTO(null, valor, null, null, null, null, null, null));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new VeiculoAtualizacaoRequestDTO(null, null, null, null, null, valor));
    }
}
