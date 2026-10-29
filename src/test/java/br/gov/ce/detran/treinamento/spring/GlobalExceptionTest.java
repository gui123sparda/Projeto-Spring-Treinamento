package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.globalExceptions.GlobalException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.ProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.VeiculoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.HistoricoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.LogProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.ConflitoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.RecursoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.RequisicaoInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionTest {
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalException())
                .alwaysExpect(jsonPath("$.dataHora", org.hamcrest.Matchers.matchesPattern(
                        "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}")))
                .alwaysExpect(jsonPath("$.excecao").isString())
                .build();
    }

    @Test
    void preservaStatusEMensagemDeRecursoNaoEncontrado() throws Exception {
        mvc.perform(get("/erros/ausente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.mensagem").value("Veículo não encontrado."))
                .andExpect(jsonPath("$.excecao").value("ResponseStatusException"));
    }

    @Test
    void informaCamposInvalidosDoCorpo() throws Exception {
        mvc.perform(post("/erros").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("nome"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Informe o nome."));
    }

    @Test
    void ocultaDetalhesDoBancoEmConflitos() throws Exception {
        mvc.perform(get("/erros/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Operação viola uma restrição de integridade dos dados."));
    }

    @Test
    void ocultaDetalhesDeFalhasInesperadas() throws Exception {
        mvc.perform(get("/erros/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro interno. Tente novamente mais tarde."));
    }

    @Test
    void trataViolacoesDeValidacaoDoService() throws Exception {
        mvc.perform(get("/erros/validacao"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("nome"));
    }

    @Test
    void rejeitaJsonInvalido() throws Exception {
        mvc.perform(post("/erros").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void trataArgumentoInvalido() throws Exception {
        mvc.perform(get("/erros/argumento"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("ID inválido."));
    }

    @ParameterizedTest
    @CsvSource({"recurso,404", "negocio,400", "duplicado,409"})
    void trataExcecoesDeNegocio(String tipo, int status) throws Exception {
        mvc.perform(get("/erros/{tipo}", tipo))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.mensagem").value("Mensagem de negócio."));
    }

    @Test
    void padronizaMetodoNaoPermitidoEPreservaCabecalhoAllow() throws Exception {
        mvc.perform(put("/erros"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @ParameterizedTest
    @CsvSource({
            "Proprietario,Proprietário não encontrado.",
            "Veiculo,Veículo não encontrado.",
            "Historico,Histórico não encontrado.",
            "LogProprietario,Log do proprietário não encontrado."
    })
    void centralizaMensagensDosRecursosNaCamadaDeExcecoes(String recurso, String mensagem) throws Exception {
        mvc.perform(get("/recurso/{recurso}", recurso))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem))
                .andExpect(jsonPath("$.excecao").value(recurso + "NaoEncontradoException"));
    }

    @Test
    void usaDataEHoraDoFusoDeFortaleza() throws Exception {
        var formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        var fuso = java.time.ZoneId.of("America/Fortaleza");
        var antes = java.time.LocalDateTime.now(fuso).withNano(0);
        var resultado = mvc.perform(get("/erros/recurso")).andReturn();
        var json = new tools.jackson.databind.ObjectMapper().readTree(resultado.getResponse().getContentAsString());
        var dataHora = java.time.LocalDateTime.parse(json.get("dataHora").asString(), formato);
        org.assertj.core.api.Assertions.assertThat(dataHora)
                .isBetween(antes, java.time.LocalDateTime.now(fuso));
    }

    record Dados(@NotBlank(message = "Informe o nome.") String nome) {}

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/recurso/{recurso}")
        void recursoAusente(@PathVariable String recurso) {
            throw switch (recurso) {
                case "Proprietario" -> new ProprietarioNaoEncontradoException();
                case "Veiculo" -> new VeiculoNaoEncontradoException();
                case "Historico" -> new HistoricoNaoEncontradoException();
                case "LogProprietario" -> new LogProprietarioNaoEncontradoException();
                default -> new IllegalArgumentException("Recurso inválido.");
            };
        }

        @PostMapping("/erros")
        void cadastrar(@RequestBody @Valid Dados dados) {}

        @GetMapping("/erros/{tipo}")
        void falhar(@PathVariable String tipo) {
            switch (tipo) {
                case "ausente" -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado.");
                case "conflito" -> throw new DataIntegrityViolationException("SQL com detalhes internos");
                case "inesperado" -> throw new IllegalStateException("Detalhes internos");
                case "argumento" -> throw new IllegalArgumentException("ID inválido.");
                case "recurso" -> throw new RecursoNaoEncontradoException("Mensagem de negócio.");
                case "negocio" -> throw new RequisicaoInvalidaException("Mensagem de negócio.");
                case "duplicado" -> throw new ConflitoException("Mensagem de negócio.");
                case "validacao" -> {
                    try (var factory = Validation.buildDefaultValidatorFactory()) {
                        throw new ConstraintViolationException(factory.getValidator().validate(new Dados(null)));
                    }
                }
            }
        }
    }
}
