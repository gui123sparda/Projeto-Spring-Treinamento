package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.controller.web.VeiculoWebController;
import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.VeiculoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VeiculoWebFluxoTest {
    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired VeiculoService veiculos;
    @Autowired ProprietarioService proprietarios;

    @Test
    void cadastraEditaEExcluiVeiculoPeloFormulario() throws Exception {
        var proprietario = proprietarios.cadastrar(new ProprietarioRequestDTO(
                "11144477735", "Teste veículo web", null, null, null, null, null, null));
        var mvc = MockMvcBuilders.standaloneSetup(new VeiculoWebController(veiculos, proprietarios)).build();
        var resultado = mvc.perform(post("/veiculos").param("tipo", "CARRO")
                        .param("idProprietario", proprietario.id().toString())
                        .param("anoFabricacao", "2020").param("chassi", "9BWZZZPZ5FP123456")
                        .param("renavam", "12345678901").param("placa", "ABC1D23"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/veiculos");
        int id = veiculos.consultarPlaca("ABC1D23").orElseThrow().id();
        String destino = "/veiculos/" + id;
        assertThat(veiculos.buscarPorId(id).placa()).isEqualTo("ABC1D23");
        mvc.perform(get(destino)).andExpect(view().name("veiculos/detalhes"))
                .andExpect(model().attributeExists("proprietario"));
        mvc.perform(get(destino + "/editar")).andExpect(view().name("veiculos/formulario"))
                .andExpect(model().attribute("edicao", true));
        mvc.perform(post(destino).param("tipo", "MOTO")
                        .param("idProprietario", proprietario.id().toString())
                        .param("anoFabricacao", "2024").param("chassi", "9BWZZZPZ5FP123456")
                        .param("renavam", "12345678901").param("placa", "XYZ9A87"))
                .andExpect(redirectedUrl("/veiculos"));
        assertThat(veiculos.buscarPorId(id).tipo()).isEqualTo("MOTO");
        assertThat(veiculos.buscarPorId(id).placa()).isEqualTo("XYZ9A87");
        mvc.perform(post(destino + "/excluir")).andExpect(redirectedUrl("/veiculos"));
        MockMvcBuilders.webAppContextSetup(webContext).build()
                .perform(get("/proprietarios/" + proprietario.id() + "/veiculos"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("proprietario", "pagina"));
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> veiculos.buscarPorId(id)).isInstanceOf(VeiculoNaoEncontradoException.class);
    }


    @Test
    void consultaPlacaRenderizaResultadoEImagemNosQuatroFormatos() throws Exception {
        var proprietario = proprietarios.cadastrar(new ProprietarioRequestDTO(
                "11144477735", "Teste placa", null, null, null, "FORTALEZA", "CE", null));
        var mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        String[][] casos = {{"CARRO", "ABC1D23", "2080", "560"},
                {"CARRO", "DEF1234", "1600", "520"},
                {"MOTO", "GHI2J34", "800", "680"}, {"MOTO", "KLM5678", "800", "680"}};
        for (int i = 0; i < casos.length; i++) {
            var caso = casos[i];
            var veiculo = veiculos.cadastrar(new br.gov.ce.detran.treinamento.spring.dto.request.VeiculoRequestDTO(
                    caso[0], proprietario.id(), 2024, "CHASSIWEB" + i, "1234567890" + i, caso[1]));
            entityManager.flush();
            var resposta = mvc.perform(get("/veiculos/consultar-placa")
                            .param("placa", caso[1].toLowerCase().substring(0, 3) + "-" + caso[1].substring(3)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(resposta).contains("hover-3d placa-hover-3d", caso[1], "TESTE PLACA", "Editar veículo")
                    .doesNotContain("th:");
            var bytes = mvc.perform(get("/veiculos/" + veiculo.id() + "/placa.png"))
                    .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                    .andReturn().getResponse().getContentAsByteArray();
            var imagem = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            assertThat(imagem.getWidth()).isEqualTo(Integer.parseInt(caso[2]));
            assertThat(imagem.getHeight()).isEqualTo(Integer.parseInt(caso[3]));
        }
        mvc.perform(get("/veiculos/consultar-placa").param("placa", "ZZZ0000"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("aviso"));
    }

    @Test
    void cadastraProprietarioERetornaAoVeiculoComSelecaoPreenchida() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        var resposta = mvc.perform(post("/proprietarios").param("origem", "veiculo")
                        .param("cpfCnpj", "11144477735").param("nome", "Novo proprietário"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        String destino = resposta.getRedirectedUrl();
        assertThat(destino).startsWith("/veiculos/novo?idProprietario=");
        var formulario = mvc.perform(get(destino)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(formulario).contains("selected=\"selected\"")
                .doesNotContain("disabled=\"disabled\"", "hidden=\"hidden\"");
    }

    @Test
    void formularioInvalidoPreservaErrosEOpcoesDeProprietarios() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new VeiculoWebController(veiculos, proprietarios)).build();
        mvc.perform(post("/veiculos").param("tipo", "CAMINHAO").param("anoFabricacao", "-1"))
                .andExpect(status().isOk()).andExpect(view().name("veiculos/formulario"))
                .andExpect(model().attributeHasFieldErrors("veiculo", "tipo", "anoFabricacao", "placa", "idProprietario"))
                .andExpect(model().attributeExists("proprietarios"));
    }
}
