package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.controller.ProprietarioController;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProprietarioController.class, properties = {
        "logging.level.br.gov.ce.detran.treinamento.spring.config.HttpLoggingFilter=INFO"
})
@ExtendWith(OutputCaptureExtension.class)
class ObservabilidadeTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProprietarioService service;

    @Test
    void registraRequisicaoComStatusDuracaoECorrelacaoSemDadosSensiveis(CapturedOutput output) throws Exception {
        var resposta = mvc.perform(get("/api/proprietarios")
                        .queryParam("cpfCnpj", "52998224725")
                        .header("Authorization", "Bearer segredo"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", matchesPattern("[a-f0-9-]{36}")))
                .andReturn().getResponse();

        assertThat(output.getOut()).contains("GET /api/proprietarios", "status=200", "duracaoMs=",
                        resposta.getHeader("X-Request-Id"))
                .doesNotContain("52998224725", "Bearer segredo");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void registraErrosDeValidacao(CapturedOutput output) throws Exception {
        mvc.perform(get("/api/proprietarios/abc")).andExpect(status().isBadRequest());
        assertThat(output.getOut()).contains("GET /api/proprietarios/abc", "status=400");
    }

    @Test
    void registraFalhasInternasELimpaCorrelacao(CapturedOutput output) throws Exception {
        when(service.buscarPorId(1)).thenThrow(new IllegalStateException("Falha simulada"));
        mvc.perform(get("/api/proprietarios/1")).andExpect(status().isInternalServerError());
        assertThat(output.getOut()).contains("GET /api/proprietarios/1", "status=500");
        assertThat(MDC.get("requestId")).isNull();
    }
}
