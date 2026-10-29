package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProprietarioConsultaTest {

    @Test
    void consultaRetornaPaginaDeDtosSemExporEntidadesNaInterface() {
        var repository = mock(ProprietarioRepository.class);
        var entidade = new Proprietario();
        entidade.setId(7);
        entidade.setCpfCnpj("11144477735");
        entidade.setNome("ANA");
        var pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entidade), pageable, 1));

        var resultado = new ProprietarioService(repository, org.mockito.Mockito.mock(br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository.class), org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class)).consultar("nome", "ana", "PF", pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).containsExactly(
                new br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO(
                        7, "11144477735", "ANA", null, null, null, null, null, null));
    }
}
