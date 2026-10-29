package br.gov.ce.detran.treinamento.spring;

import static org.assertj.core.api.Assertions.*;

import br.gov.ce.detran.treinamento.spring.util.Documento;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoTest {
    @ParameterizedTest
    @ValueSource(
            strings = {"529.982.247-25", "11144477735", "11.222.333/0001-81", "12.ABC.345/01DE-35"})
    void aceitaDocumentosValidos(String valor) {
        assertThat(Documento.normalizarCpfCnpj(valor)).doesNotContain(".", "-", "/");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "11111111111",
                "00000000000000",
                "52998224724",
                "11222333000180",
                "ABC",
                "12ABC34501DE00"
            })
    void rejeitaDocumentosInvalidos(String valor) {
        assertThatIllegalArgumentException().isThrownBy(() -> Documento.normalizarCpfCnpj(valor));
    }
}
