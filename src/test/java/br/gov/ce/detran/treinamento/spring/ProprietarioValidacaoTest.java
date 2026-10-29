package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProprietarioValidacaoTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void fecharValidator() {
        FACTORY.close();
    }

    @Test
    void normalizaDocumentoNomeEEnderecoComoNoServlet() {
        var dto = new ProprietarioRequestDTO("529.982.247-25", " João Silva ",
                " Rua A ", " 10a ", "60.000-000", " Fortaleza ", " ce ", " casa ");
        assertThat(VALIDATOR.validate(dto)).isEmpty();
        assertThat(dto.cpfCnpj()).isEqualTo("52998224725");
        assertThat(dto.nome()).isEqualTo("JOÃO SILVA");
        assertThat(dto.rua()).isEqualTo("RUA A");
        assertThat(dto.numero()).isEqualTo("10A");
        assertThat(dto.cep()).isEqualTo("60000000");
        assertThat(dto.cidade()).isEqualTo("FORTALEZA");
        assertThat(dto.estado()).isEqualTo("CE");
        assertThat(dto.complemento()).isEqualTo("CASA");
    }

    @Test
    void enderecoOpcionalEmBrancoViraNulo() {
        var dto = new ProprietarioRequestDTO("11144477735", "Ana", " ", "", null, "", " ", "");
        assertThat(VALIDATOR.validate(dto)).isEmpty();
        assertThat(dto.rua()).isNull();
        assertThat(dto.numero()).isNull();
        assertThat(dto.cep()).isNull();
        assertThat(dto.cidade()).isNull();
        assertThat(dto.estado()).isNull();
        assertThat(dto.complemento()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "11111111111", "00000000000000", "52998224724",
            "11222333000180", "ABC", "12ABC34501DE00"})
    void rejeitaDocumentoAusenteOuInvalido(String documento) {
        assertThat(VALIDATOR.validate(dto(documento, "Ana")))
                .extracting(v -> v.getPropertyPath().toString()).contains("cpfCnpj");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "12.abc.345/01de-35"})
    void aceitaCnpjNumericoEAlfanumerico(String documento) {
        assertThat(VALIDATOR.validate(dto(documento, "Empresa"))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void nomeObrigatorio(String nome) {
        assertThat(VALIDATOR.validate(dto("11144477735", nome)))
                .extracting(v -> v.getPropertyPath().toString()).contains("nome");
    }

    @Test
    void aceitaLimitesDeTamanhoDoServlet() {
        var dto = new ProprietarioRequestDTO("11144477735", "A".repeat(150),
                "R".repeat(150), "N".repeat(20), "60000000", "C".repeat(100), "CE", "X".repeat(255));
        assertThat(VALIDATOR.validate(dto)).isEmpty();
    }

    @Test
    void rejeitaCamposAcimaDosLimites() {
        var dto = new ProprietarioRequestDTO("11144477735", "A".repeat(151),
                "R".repeat(151), "N".repeat(21), "123", "C".repeat(101), "CEARA", "X".repeat(256));
        assertThat(VALIDATOR.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .contains("nome", "rua", "numero", "cep", "cidade", "estado", "complemento");
    }

    @ParameterizedTest
    @CsvSource({"1234567,CE", "123456789,CE", "ABCDEFGH,CE", "60000000,C1", "60000000,C"})
    void rejeitaFormatoDeCepOuEstado(String cep, String estado) {
        var dto = new ProprietarioRequestDTO("11144477735", "Ana", null, null, cep, null, estado, null);
        assertThat(VALIDATOR.validate(dto)).isNotEmpty();
    }

    @Test
    void entidadeTambemExigeDocumentoENome() {
        assertThat(VALIDATOR.validate(new Proprietario()))
                .extracting(v -> v.getPropertyPath().toString()).contains("nome", "cpfCnpj");
    }

    @Test
    void normalizaEntidadeAntesDePersistir() {
        var proprietario = new Proprietario();
        proprietario.setCpfCnpj("12.abc.345/01de-35");
        proprietario.setNome(" Empresa ");
        proprietario.setRua(" Rua A ");
        proprietario.setNumero(" 10a ");
        proprietario.setCep("60.000-000");
        proprietario.setCidade(" Fortaleza ");
        proprietario.setEstado(" ce ");
        proprietario.setComplemento(" ");
        proprietario.normalizarDados();

        assertThat(VALIDATOR.validate(proprietario)).isEmpty();
        assertThat(proprietario.getCpfCnpj()).isEqualTo("12ABC34501DE35");
        assertThat(proprietario.getNome()).isEqualTo("EMPRESA");
        assertThat(proprietario.getRua()).isEqualTo("RUA A");
        assertThat(proprietario.getNumero()).isEqualTo("10A");
        assertThat(proprietario.getCep()).isEqualTo("60000000");
        assertThat(proprietario.getCidade()).isEqualTo("FORTALEZA");
        assertThat(proprietario.getEstado()).isEqualTo("CE");
        assertThat(proprietario.getComplemento()).isNull();
    }

    @Test
    void entidadeRejeitaDocumentoEEnderecoInvalidos() {
        var proprietario = new Proprietario();
        proprietario.setId(-1);
        proprietario.setCpfCnpj("52998224724");
        proprietario.setNome("A".repeat(151));
        proprietario.setRua("R".repeat(151));
        proprietario.setNumero("N".repeat(21));
        proprietario.setCep("123");
        proprietario.setCidade("C".repeat(101));
        proprietario.setEstado("CEARA");
        proprietario.setComplemento("X".repeat(256));
        assertThat(VALIDATOR.validate(proprietario)).extracting(v -> v.getPropertyPath().toString())
                .contains("id", "cpfCnpj", "nome", "rua", "numero", "cep", "cidade", "estado", "complemento");
    }

    private static ProprietarioRequestDTO dto(String documento, String nome) {
        return new ProprietarioRequestDTO(documento, nome, null, null, null, null, null, null);
    }
}
