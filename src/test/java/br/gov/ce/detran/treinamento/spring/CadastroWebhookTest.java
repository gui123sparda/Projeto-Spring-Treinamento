package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.*;
import br.gov.ce.detran.treinamento.spring.model.*;
import br.gov.ce.detran.treinamento.spring.repository.*;
import br.gov.ce.detran.treinamento.spring.service.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CadastroWebhookTest {
    private final BlockingQueue<Recebido> recebidos = new LinkedBlockingQueue<>();
    private HttpServer servidor;
    private AnnotationConfigApplicationContext contexto;
    private TransactionTemplate transacao;
    private int status = 204;
    private CountDownLatch liberarResposta;
    private final ObjectMapper json = new ObjectMapper();

    record Recebido(String metodo, String contentType, String autorizacao, String corpo) {}

    @BeforeEach
    void preparar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/webhook", exchange -> {
            recebidos.add(new Recebido(exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            try {
                if (liberarResposta != null) liberarResposta.await(5, TimeUnit.SECONDS);
                exchange.sendResponseHeaders(status, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        servidor.start();
        iniciar("http://127.0.0.1:" + servidor.getAddress().getPort() + "/webhook");
    }

    private void iniciar(String url) {
        contexto = new AnnotationConfigApplicationContext();
        contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("webhook-test", Map.of(
                "app.webhook.url", url, "app.webhook.token", "token-teste")));
        contexto.register(Config.class, ProprietarioService.class, VeiculoService.class);
        contexto.refresh();
        transacao = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        var p = new Proprietario();
        p.setId(42); p.setNome("MARIA"); p.setCpfCnpj("52998224725");
        var v = new Veiculo();
        v.setId(73); v.setIdProprietario(p); v.setTipo(TipoVeiculo.CARRO);
        v.setAnoFabricacao(2024); v.setPlaca("ABC1D23");
        var proprietarios = contexto.getBean(ProprietarioRepository.class);
        var veiculos = contexto.getBean(VeiculoRepository.class);
        when(proprietarios.findById(42)).thenReturn(Optional.of(p));
        when(veiculos.findById(73)).thenReturn(Optional.of(v));
        when(proprietarios.save(any())).thenAnswer(i -> {
            Proprietario salvo = i.getArgument(0); salvo.setId(42); return salvo;
        });
        when(veiculos.save(any())).thenAnswer(i -> {
            Veiculo salvo = i.getArgument(0); salvo.setId(73); return salvo;
        });
    }

    @AfterEach
    void encerrar() {
        if (liberarResposta != null) liberarResposta.countDown();
        contexto.close();
        servidor.stop(0);
    }

    @ParameterizedTest
    @CsvSource({"proprietario,CADASTRO,42", "proprietario,ALTERACAO,42", "proprietario,EXCLUSAO_LOGICA,42",
            "veiculo,CADASTRO,73", "veiculo,ALTERACAO,73", "veiculo,EXCLUSAO_LOGICA,73"})
    void enviaEventoSomenteDepoisDoCommit(String entidade, String operacao, int id) throws Exception {
        transacao.executeWithoutResult(tx -> {
            alterar(entidade, operacao);
            assertThat(recebidos).isEmpty();
        });
        var recebido = recebidos.poll(3, TimeUnit.SECONDS);
        assertThat(recebido).as("POST do webhook após o commit").isNotNull();
        assertThat(recebido.metodo()).isEqualTo("POST");
        assertThat(recebido.contentType()).startsWith("application/json");
        assertThat(recebido.autorizacao()).isEqualTo("Bearer token-teste");
        var evento = json.readTree(recebido.corpo());
        assertThat(evento.get("entidade").asText()).isEqualTo(entidade);
        assertThat(evento.get("operacao").asText()).isEqualTo(operacao);
        assertThat(evento.get("registroId").asInt()).isEqualTo(id);
        assertThat(UUID.fromString(evento.get("eventoId").asText())).isNotNull();
        assertThat(OffsetDateTime.parse(evento.get("ocorridoEm").asText())).isNotNull();
        assertThat(recebido.corpo()).doesNotContain("descricao", "cpfCnpj", "MARIA", "ANA", "52998224725");
        assertThat(recebidos.poll(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void rollbackNaoEnviaEvento() throws Exception {
        transacao.executeWithoutResult(tx -> {
            alterar("veiculo", "CADASTRO");
            tx.setRollbackOnly();
        });
        assertThat(recebidos.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void falhaNaPersistenciaNaoEnviaEvento() throws Exception {
        doThrow(new IllegalStateException("Falha")).when(contexto.getBean(VeiculoRepository.class)).save(any());
        assertThatThrownBy(() -> transacao.executeWithoutResult(tx -> alterar("veiculo", "CADASTRO")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(recebidos.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void webhookSemUrlFicaDesativado() throws Exception {
        contexto.close();
        iniciar("");
        transacao.executeWithoutResult(tx -> alterar("proprietario", "CADASTRO"));
        assertThat(recebidos.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void respostaDeErroDoDestinoNaoFalhaOperacao() throws Exception {
        status = 500;
        assertThatCode(() -> transacao.executeWithoutResult(tx -> alterar("veiculo", "EXCLUSAO_LOGICA")))
                .doesNotThrowAnyException();
        assertThat(recebidos.poll(3, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void envioNaoEsperaRespostaDoDestino() throws Exception {
        liberarResposta = new CountDownLatch(1);
        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(1),
                () -> transacao.executeWithoutResult(tx -> alterar("proprietario", "CADASTRO")));
        assertThat(recebidos.poll(3, TimeUnit.SECONDS)).isNotNull();
    }

    private void alterar(String entidade, String operacao) {
        var p = contexto.getBean(ProprietarioService.class);
        var v = contexto.getBean(VeiculoService.class);
        if (entidade.equals("proprietario")) {
            switch (operacao) {
                case "CADASTRO" -> p.cadastrar(new ProprietarioRequestDTO("52998224725", "MARIA", null, null, null, null, null, null));
                case "ALTERACAO" -> p.atualizar(42, new ProprietarioAtualizacaoRequestDTO(null, "ANA", null, null, null, null, null, null));
                case "EXCLUSAO_LOGICA" -> p.excluir(42);
            }
        } else {
            switch (operacao) {
                case "CADASTRO" -> v.cadastrar(new VeiculoRequestDTO("CARRO", 42, 2024, "123", "456", "ABC1D23"));
                case "ALTERACAO" -> v.atualizar(73, new VeiculoAtualizacaoRequestDTO(null, null, null, null, null, "XYZ9A87"));
                case "EXCLUSAO_LOGICA" -> v.excluir(73);
            }
        }
    }

    @TestConfiguration
    @EnableTransactionManagement
    @ComponentScan("br.gov.ce.detran.treinamento.spring.webhook")
    static class Config {
        @Bean ProprietarioRepository proprietarios() { return mock(ProprietarioRepository.class); }
        @Bean VeiculoRepository veiculos() { return mock(VeiculoRepository.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        // Exercita os eventos transacionais reais do Spring sem gravar no banco.
        @Bean PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                class Transacao implements SmartTransactionObject {
                    boolean rollbackOnly;
                    public boolean isRollbackOnly() { return rollbackOnly; }
                    public void flush() {}
                }
                private final ThreadLocal<Transacao> atual = new ThreadLocal<>();
                protected Object doGetTransaction() {
                    return atual.get() == null ? new Transacao() : atual.get();
                }
                protected boolean isExistingTransaction(Object tx) { return atual.get() == tx; }
                protected void doBegin(Object tx, TransactionDefinition definition) { atual.set((Transacao) tx); }
                protected void doCommit(DefaultTransactionStatus status) {}
                protected void doRollback(DefaultTransactionStatus status) {}
                protected void doSetRollbackOnly(DefaultTransactionStatus status) {
                    ((Transacao) status.getTransaction()).rollbackOnly = true;
                }
                protected void doCleanupAfterCompletion(Object tx) { atual.remove(); }
            };
        }
    }
}
