package br.gov.ce.detran.treinamento.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerTest {
    @Autowired MockMvc mvc;

    @Test
    void documentaSomenteApiRestComNomeDoProjeto() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Treinamento CRUD Spring Boot"))
                .andExpect(jsonPath("$.paths", hasKey("/api/proprietarios")))
                .andExpect(jsonPath("$.paths", hasKey("/api/veiculos")))
                .andExpect(jsonPath("$.paths", hasKey("/api/historico")))
                .andExpect(jsonPath("$.paths", hasKey("/api/log-proprietarios")))
                .andExpect(jsonPath("$.paths", not(hasKey("/veiculos"))))
                .andExpect(jsonPath("$.paths", not(hasKey("/notificacoes"))));
    }

    @Test
    void disponibilizaInterfaceSwagger() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
