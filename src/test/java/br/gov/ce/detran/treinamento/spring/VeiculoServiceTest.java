package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.ProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.VeiculoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import br.gov.ce.detran.treinamento.spring.model.Veiculo;
import br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository;
import br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository;
import br.gov.ce.detran.treinamento.spring.service.VeiculoService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VeiculoServiceTest {
    private final VeiculoRepository repository = mock(VeiculoRepository.class);
    private final ProprietarioRepository proprietarios = mock(ProprietarioRepository.class);
    private final VeiculoService service = new VeiculoService(repository, proprietarios, mock(org.springframework.context.ApplicationEventPublisher.class));

    @Test
    void atualizaApenasPlacaPreservandoDemaisCampos() {
        var veiculo = veiculo();
        when(repository.findById(1)).thenReturn(Optional.of(veiculo));
        when(repository.save(veiculo)).thenReturn(veiculo);

        var resposta = service.atualizar(1,
                new VeiculoAtualizacaoRequestDTO(null, null, null, null, null, " xyz9a87 "));

        assertThat(resposta.placa()).isEqualTo("XYZ9A87");
        assertThat(resposta.tipo()).isEqualTo("CARRO");
        assertThat(resposta.anoFabricacao()).isEqualTo(2020);
        assertThat(resposta.chassi()).isEqualTo("9BWZZZPZ5FP123456");
        assertThat(resposta.renavam()).isEqualTo("12345678901");
        assertThat(resposta.idProprietario()).isEqualTo(2);
    }

    @Test
    void atualizaTodosOsCamposInformados() {
        var veiculo = veiculo();
        var proprietario = new Proprietario();
        proprietario.setId(3);
        when(repository.findById(1)).thenReturn(Optional.of(veiculo));
        when(proprietarios.findById(3)).thenReturn(Optional.of(proprietario));
        when(repository.save(veiculo)).thenReturn(veiculo);

        var resposta = service.atualizar(1,
                new VeiculoAtualizacaoRequestDTO("moto", 3, 2024, "novo", "98765432100", "XYZ9A87"));

        assertThat(resposta.tipo()).isEqualTo("MOTO");
        assertThat(resposta.idProprietario()).isEqualTo(3);
        assertThat(resposta.anoFabricacao()).isEqualTo(2024);
        assertThat(resposta.chassi()).isEqualTo("NOVO");
        assertThat(resposta.renavam()).isEqualTo("98765432100");
        assertThat(resposta.placa()).isEqualTo("XYZ9A87");
    }

    @Test
    void rejeitaProprietarioInexistenteSemAlterarVeiculo() {
        var veiculo = veiculo();
        when(repository.findById(1)).thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> service.atualizar(1,
                new VeiculoAtualizacaoRequestDTO("MOTO", 3, null, null, null, null)))
                .isInstanceOf(ProprietarioNaoEncontradoException.class);
        assertThat(veiculo.getTipo()).isEqualTo(br.gov.ce.detran.treinamento.spring.model.TipoVeiculo.CARRO);
        assertThat(veiculo.getIdProprietario().getId()).isEqualTo(2);
    }

    @Test
    void rejeitaAtualizacaoDeVeiculoInexistente() {
        assertThatThrownBy(() -> service.atualizar(1,
                new VeiculoAtualizacaoRequestDTO(null, null, null, null, null, "XYZ9A87")))
                .isInstanceOf(VeiculoNaoEncontradoException.class);
    }

    @Test
    void excluiVeiculoExistente() {
        var veiculo = veiculo();
        when(repository.findById(1)).thenReturn(Optional.of(veiculo));

        service.excluir(1);

        assertThat(veiculo.getExcluidoEm()).isNotNull();
        verify(repository, never()).delete(veiculo);
    }

    @Test
    void rejeitaExclusaoDeVeiculoInexistente() {
        assertThatThrownBy(() -> service.excluir(1))
                .isInstanceOf(VeiculoNaoEncontradoException.class);
    }

    private Veiculo veiculo() {
        var proprietario = new Proprietario();
        proprietario.setId(2);
        var veiculo = new Veiculo();
        veiculo.setId(1);
        veiculo.setIdProprietario(proprietario);
        veiculo.setTipo(br.gov.ce.detran.treinamento.spring.model.TipoVeiculo.CARRO);
        veiculo.setAnoFabricacao(2020);
        veiculo.setChassi("9BWZZZPZ5FP123456");
        veiculo.setRenavam("12345678901");
        veiculo.setPlaca("ABC1D23");
        return veiculo;
    }
}
