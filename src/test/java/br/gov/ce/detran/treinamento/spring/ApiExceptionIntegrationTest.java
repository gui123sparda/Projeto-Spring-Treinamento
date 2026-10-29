package br.gov.ce.detran.treinamento.spring;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiExceptionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    @Test
    void listaProprietariosECadastraComDadosNormalizados() throws Exception {
        int id = criarProprietario();
        mvc.perform(get("/api/proprietarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].nome").value("JOÃO SILVA"));
        assertThat(jdbc.queryForObject("select count(*) from treino.log_proprietario where id_proprietario=?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from treino.historico where entidade='proprietario' and registro_id=?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void patchProprietarioPreservaDadosNaoInformados() throws Exception {
        int id = criarProprietario();
        mvc.perform(patch("/api/proprietarios/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"cidade\":\" recife \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("RECIFE"))
                .andExpect(jsonPath("$.nome").value("JOÃO SILVA"))
                .andExpect(jsonPath("$.cpfCnpj").value("52998224725"));
        sincronizar();
        assertThat(jdbc.queryForObject("select count(*) from treino.historico where entidade='proprietario' and registro_id=?", Integer.class, id)).isEqualTo(2);
    }

    @Test
    void rejeitaAlteracaoDoCpfCnpj() throws Exception {
        int id = criarProprietario();
        mvc.perform(patch("/api/proprietarios/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"cpfCnpj\":\"11144477735\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void persisteTipoDoVeiculoEPermitePatchParcial() throws Exception {
        int id = criarVeiculo(criarProprietario(), 1);
        sincronizar();
        mvc.perform(get("/api/veiculos/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("CARRO"));
        mvc.perform(patch("/api/veiculos/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"tipo\":\"MOTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MOTO"))
                .andExpect(jsonPath("$.placa").value("ABC0001"));
        sincronizar();
        assertThat(jdbc.queryForObject("select tipo::text from treino.veiculo where id=?", String.class, id)).isEqualTo("MOTO");
    }

    @Test
    void excluiVeiculoLogicamenteEOcultaRegistroInativo() throws Exception {
        int id = criarVeiculo(criarProprietario(), 1);
        mvc.perform(delete("/api/veiculos/{id}", id)).andExpect(status().isNoContent());
        sincronizar();
        assertThat(jdbc.queryForObject("select excluido_em is not null from treino.veiculo where id=?", Boolean.class, id)).isTrue();
        mvc.perform(get("/api/veiculos/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/veiculos")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/api/veiculos/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/veiculos/{id}", id).contentType(APPLICATION_JSON).content("{\"tipo\":\"MOTO\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exclusaoLogicaDoProprietarioInativaVeiculosPelaTrigger() throws Exception {
        int proprietario = criarProprietario();
        int veiculo = criarVeiculo(proprietario, 1);
        mvc.perform(delete("/api/proprietarios/{id}", proprietario)).andExpect(status().isNoContent());
        sincronizar();
        mvc.perform(get("/api/proprietarios/{id}", proprietario)).andExpect(status().isNotFound());
        mvc.perform(get("/api/proprietarios")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/veiculos/{id}", veiculo)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("""
                select count(*) from treino.historico
                where operacao='EXCLUSAO_LOGICA'
                  and ((entidade='proprietario' and registro_id=?)
                    or (entidade='veiculo' and registro_id=?))
                """, Integer.class, proprietario, veiculo)).isEqualTo(2);
    }

    @Test
    void rejeitaVinculoComProprietarioInativo() throws Exception {
        int id = criarProprietario();
        mvc.perform(delete("/api/proprietarios/{id}", id)).andExpect(status().isNoContent());
        sincronizar();
        mvc.perform(post("/api/veiculos").contentType(APPLICATION_JSON).content(veiculo(id, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void limitaPaginaAQuinzeEPreservaNavegacao() throws Exception {
        int proprietario = criarProprietario();
        for (int i = 1; i <= 17; i++) criarVeiculo(proprietario, i);
        mvc.perform(get("/api/veiculos").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(15))
                .andExpect(jsonPath("$.totalElements").value(17));
        mvc.perform(get("/api/veiculos").param("size", "100").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/veiculos").param("size", "5"))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void auditoriaDisponivelSomenteParaConsulta() throws Exception {
        criarProprietario();
        for (String rota : new String[]{"/api/historico", "/api/log-proprietarios"}) {
            mvc.perform(get(rota)).andExpect(status().isOk());
            mvc.perform(post(rota).contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Test
    void rejeitaTipoDeVeiculoDesconhecido() throws Exception {
        int proprietario = criarProprietario();
        mvc.perform(post("/api/veiculos").contentType(APPLICATION_JSON)
                        .content(veiculo(proprietario, 1).replace("CARRO", "AVIAO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validaCorpoEIdentificadores() throws Exception {
        mvc.perform(post("/api/proprietarios").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/proprietarios/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/veiculos/2147483647")).andExpect(status().isNotFound());
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    private int criarProprietario() throws Exception {
        var resposta = mvc.perform(post("/api/proprietarios").contentType(APPLICATION_JSON)
                        .content("{\"cpfCnpj\":\"529.982.247-25\",\"nome\":\" João Silva \"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString()).get("id").asInt();
    }

    private int criarVeiculo(int proprietario, int numero) throws Exception {
        var resposta = mvc.perform(post("/api/veiculos").contentType(APPLICATION_JSON).content(veiculo(proprietario, numero)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString()).get("id").asInt();
    }

    private String veiculo(int proprietario, int numero) {
        return """
                {"tipo":"CARRO","idProprietario":%d,"anoFabricacao":2020,
                 "chassi":"%017d","renavam":"%011d","placa":"ABC%04d"}
                """.formatted(proprietario, numero, numero, numero);
    }
}
