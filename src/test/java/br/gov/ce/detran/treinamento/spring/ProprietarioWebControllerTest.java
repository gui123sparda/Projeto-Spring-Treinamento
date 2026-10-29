package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.controller.web.ProprietarioWebController;
import br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ProprietarioWebControllerTest {

    @Test
    void listaProprietariosComFiltrosNaCamadaDeServico() throws Exception {
        var service = mock(ProprietarioService.class);
        var pagina = new PageImpl<>(List.of(proprietario()), PageRequest.of(0, 10), 1);
        when(service.consultar("nome", "ANA", "PF", PageRequest.of(0, 10))).thenReturn(pagina);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProprietarioWebController(service)).build();

        mvc.perform(get("/proprietarios")
                        .param("campo", "nome")
                        .param("valor", "ANA")
                        .param("tipoPessoa", "PF"))
                .andExpect(status().isOk())
                .andExpect(view().name("proprietarios/lista"))
                .andExpect(model().attribute("pagina", pagina))
                .andExpect(model().attribute("campo", "nome"))
                .andExpect(model().attribute("valor", "ANA"))
                .andExpect(model().attribute("tipoPessoa", "PF"));
    }

    @Test
    void abreFormularioDeNovoProprietario() throws Exception {
        var service = mock(ProprietarioService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProprietarioWebController(service)).build();

        mvc.perform(get("/proprietarios/novo").param("tipoPessoa", "PJ"))
                .andExpect(status().isOk())
                .andExpect(view().name("proprietarios/formulario"))
                .andExpect(model().attribute("tipoPessoa", "PJ"))
                .andExpect(model().attributeExists("proprietario"));
    }

    private static ProprietarioResponseDTO proprietario() {
        return new ProprietarioResponseDTO(1, "11144477735", "ANA", "RUA A", "10",
                "60000000", "FORTALEZA", "CE", null);
    }
}
