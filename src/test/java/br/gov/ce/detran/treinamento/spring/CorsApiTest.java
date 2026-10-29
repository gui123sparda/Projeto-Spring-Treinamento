package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.controller.ProprietarioController;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProprietarioController.class)
class CorsApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProprietarioService service;

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:54321", "http://127.0.0.1:5173"})
    void navegadorLocalPodeLerAApi(String origem) throws Exception {
        mvc.perform(get("/api/proprietarios").header("Origin", origem))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origem))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH", "DELETE"})
    void permitePreflightDosFormularios(String metodo) throws Exception {
        mvc.perform(options("/api/proprietarios/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", metodo)
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString(metodo)))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("content-type")));
    }

    @Test
    void naoAutorizaOrigemExterna() throws Exception {
        mvc.perform(options("/api/proprietarios")
                        .header("Origin", "http://localhost.site-externo.example:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void chamadasSemOriginContinuamFuncionando() throws Exception {
        mvc.perform(get("/api/proprietarios"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
