package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class NormalizacaoTest {
    @Test
    void campoAusenteNaAtualizacaoPermaneceNulo() {
        assertThat(Normalizacao.campoAtualizacao(null)).isNull();
    }

    @Test
    void normalizaCampoInformadoNaAtualizacao() {
        assertThat(Normalizacao.campoAtualizacao(" João da Silva ")).isEqualTo("JOÃO DA SILVA");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t", "\r\n"})
    void campoEmBrancoNaAtualizacaoNaoPodeSerTratadoComoAusente(String valor) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Normalizacao.campoAtualizacao(valor));
    }
}
