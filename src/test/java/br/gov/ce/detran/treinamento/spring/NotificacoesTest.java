package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.controller.ProprietarioController;
import br.gov.ce.detran.treinamento.spring.controller.VeiculoController;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import br.gov.ce.detran.treinamento.spring.service.VeiculoService;
import br.gov.ce.detran.treinamento.spring.webhook.CadastroEvento;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class NotificacoesTest {
    private AnnotationConfigWebApplicationContext contexto;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void preparar() {
        contexto = new AnnotationConfigWebApplicationContext();
        contexto.setServletContext(new MockServletContext());
        contexto.register(Config.class, CadastroWebhookTest.Config.class,
                ProprietarioService.class, VeiculoService.class,
                ProprietarioController.class, VeiculoController.class);
        contexto.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        var p = new br.gov.ce.detran.treinamento.spring.model.Proprietario();
        p.setId(42); p.setNome("MARIA"); p.setCpfCnpj("52998224725");
        var v = new br.gov.ce.detran.treinamento.spring.model.Veiculo();
        v.setId(73); v.setIdProprietario(p); v.setTipo(br.gov.ce.detran.treinamento.spring.model.TipoVeiculo.CARRO);
        v.setAnoFabricacao(2024); v.setPlaca("ABC1D23");
        var proprietarios = contexto.getBean(br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository.class);
        var veiculos = contexto.getBean(br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository.class);
        when(proprietarios.findById(42)).thenReturn(java.util.Optional.of(p));
        when(veiculos.findById(73)).thenReturn(java.util.Optional.of(v));
        when(proprietarios.save(any())).thenAnswer(i -> { var salvo = (br.gov.ce.detran.treinamento.spring.model.Proprietario) i.getArgument(0); salvo.setId(42); return salvo; });
        when(veiculos.save(any())).thenAnswer(i -> { var salvo = (br.gov.ce.detran.treinamento.spring.model.Veiculo) i.getArgument(0); salvo.setId(73); return salvo; });
    }

    @AfterEach
    void encerrar() { RequestContextHolder.resetRequestAttributes(); contexto.close(); }

    @Test
    void navegadorRecebeNomeOuPlacaDoMomentoDaOperacaoInclusiveAposExclusao() throws Exception {
        String cursor = cursorInicial();
        mvc.perform(post("/api/proprietarios").contentType(APPLICATION_JSON)
                .content("{\"cpfCnpj\":\"52998224725\",\"nome\":\"MARIA\"}")).andExpect(status().isCreated());
        mvc.perform(patch("/api/proprietarios/42").contentType(APPLICATION_JSON)
                .content("{\"nome\":\"ANA\"}")).andExpect(status().isOk());
        mvc.perform(delete("/api/proprietarios/42")).andExpect(status().isNoContent());
        mvc.perform(post("/api/veiculos").contentType(APPLICATION_JSON)
                .content("{\"tipo\":\"CARRO\",\"idProprietario\":42,\"anoFabricacao\":2024,\"placa\":\"ABC1D23\",\"chassi\":\"123\",\"renavam\":\"456\"}"))
                .andExpect(status().isCreated());
        mvc.perform(patch("/api/veiculos/73").contentType(APPLICATION_JSON)
                .content("{\"placa\":\"XYZ9A87\"}")).andExpect(status().isOk());
        mvc.perform(delete("/api/veiculos/73")).andExpect(status().isNoContent());
        var resposta = mvc.perform(get("/notificacoes").param("apos", cursor))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.eventos.length()").value(6)).andReturn().getResponse().getContentAsString();
        var eventos = json.readTree(resposta).get("eventos");
        for (int i = 0; i < 6; i++) {
            assertThat(eventos.get(i).get("entidade").asText()).isEqualTo(i < 3 ? "proprietario" : "veiculo");
            assertThat(eventos.get(i).get("operacao").asText()).isEqualTo(new String[]{"CADASTRO", "ALTERACAO", "EXCLUSAO_LOGICA"}[i % 3]);
            assertThat(eventos.get(i).path("descricao").asText())
                    .isEqualTo(new String[]{"MARIA", "ANA", "ANA", "ABC1D23", "XYZ9A87", "XYZ9A87"}[i]);
        }
        assertThat(resposta).doesNotContain("52998224725", "cpfCnpj", "chassi", "renavam");
        String novoCursor = json.readTree(resposta).get("cursor").asText();
        mvc.perform(get("/notificacoes").param("apos", novoCursor)).andExpect(jsonPath("$.eventos").isEmpty());
        mvc.perform(get("/notificacoes")).andExpect(jsonPath("$.eventos").isEmpty());
    }

    @Test
    void soPublicaAposCommitEIgnoraRollbackEFormularioWeb() throws Exception {
        String cursor = cursorInicial();
        var transacao = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        var request = new MockHttpServletRequest("POST", "/api/veiculos");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        transacao.executeWithoutResult(tx -> {
            contexto.publishEvent(CadastroEvento.novo("veiculo", "CADASTRO", 73));
            try { mvc.perform(get("/notificacoes").param("apos", cursor)).andExpect(jsonPath("$.eventos").isEmpty()); }
            catch (Exception e) { throw new RuntimeException(e); }
            tx.setRollbackOnly();
        });
        mvc.perform(get("/notificacoes").param("apos", cursor)).andExpect(jsonPath("$.eventos").isEmpty());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/veiculos")));
        transacao.executeWithoutResult(tx -> contexto.publishEvent(CadastroEvento.novo("veiculo", "CADASTRO", 73)));
        mvc.perform(get("/notificacoes").param("apos", cursor)).andExpect(jsonPath("$.eventos").isEmpty());
    }

    @Test
    void retomaConsultaAposDesconexaoOuReinicioComHistoricoLimitado() throws Exception {
        String cursor = cursorInicial();
        var request = new MockHttpServletRequest("POST", "/sistema/api/veiculos");
        request.setContextPath("/sistema");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var transacao = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        for (int i = 1; i <= 205; i++) {
            int id = i;
            transacao.executeWithoutResult(tx -> contexto.publishEvent(CadastroEvento.novo("veiculo", "ALTERACAO", id)));
        }
        mvc.perform(get("/notificacoes").param("apos", cursor))
                .andExpect(jsonPath("$.eventos.length()").value(200))
                .andExpect(jsonPath("$.eventos[0].registroId").value(6))
                .andExpect(jsonPath("$.eventos[199].registroId").value(205));
    }

    private String cursorInicial() throws Exception {
        var resposta = mvc.perform(get("/notificacoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos").isEmpty()).andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("cursor").asText();
    }

    @Configuration
    @EnableWebMvc
    @ComponentScan("br.gov.ce.detran.treinamento.spring.notificacao")
    static class Config {}
}
