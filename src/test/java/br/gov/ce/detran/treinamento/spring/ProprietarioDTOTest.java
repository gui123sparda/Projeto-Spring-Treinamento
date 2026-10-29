package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProprietarioDTOTest {
    @Test
    void requestCriaEntidadeComDadosNormalizados() {
        var request = new ProprietarioRequestDTO("529.982.247-25", " João Silva ",
                " Rua A ", " 10a ", "60.000-000", " Fortaleza ", " ce ", " casa ");

        Proprietario proprietario = request.toEntity();

        assertThat(proprietario.getId()).isNull();
        assertThat(proprietario.getCpfCnpj()).isEqualTo("52998224725");
        assertThat(proprietario.getNome()).isEqualTo("JOÃO SILVA");
        assertThat(proprietario.getRua()).isEqualTo("RUA A");
        assertThat(proprietario.getNumero()).isEqualTo("10A");
        assertThat(proprietario.getCep()).isEqualTo("60000000");
        assertThat(proprietario.getCidade()).isEqualTo("FORTALEZA");
        assertThat(proprietario.getEstado()).isEqualTo("CE");
        assertThat(proprietario.getComplemento()).isEqualTo("CASA");
    }

    @Test
    void responseRetornaIdEDadosDaEntidade() {
        var proprietario = new Proprietario();
        proprietario.setId(42);
        proprietario.setCpfCnpj("11144477735");
        proprietario.setNome("ANA");
        proprietario.setRua("RUA B");
        proprietario.setNumero("20");
        proprietario.setCep("60000001");
        proprietario.setCidade("FORTALEZA");
        proprietario.setEstado("CE");
        proprietario.setComplemento("APTO 2");

        assertThat(ProprietarioResponseDTO.fromEntity(proprietario)).isEqualTo(
                new ProprietarioResponseDTO(42, "11144477735", "ANA", "RUA B", "20",
                        "60000001", "FORTALEZA", "CE", "APTO 2"));
    }

    @Test
    void responsePreservaCamposOpcionaisNulos() {
        var proprietario = new Proprietario();
        proprietario.setId(1);
        proprietario.setCpfCnpj("11144477735");
        proprietario.setNome("ANA");

        assertThat(ProprietarioResponseDTO.fromEntity(proprietario)).isEqualTo(
                new ProprietarioResponseDTO(1, "11144477735", "ANA", null, null,
                        null, null, null, null));
    }
}
