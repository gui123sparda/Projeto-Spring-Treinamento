package br.gov.ce.detran.treinamento.spring.webhook;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Component
public class CadastroWebhook {
    private static final Logger log = LoggerFactory.getLogger(CadastroWebhook.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private final URI destino;
    private final String token;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    // O webhook externo continua enviando apenas os metadados do evento.
    private record Payload(String eventoId, String entidade, String operacao, Integer registroId, String ocorridoEm) {}

    public CadastroWebhook(@Value("${app.webhook.url:}") String url,
                           @Value("${app.webhook.token:}") String token,
                           ObjectMapper json) {
        this.destino = url.isBlank() ? null : URI.create(url.trim());
        if (destino != null && (!Set.of("http", "https").contains(destino.getScheme())
                || destino.getHost() == null || destino.getUserInfo() != null || destino.getFragment() != null)) {
            throw new IllegalArgumentException("WEBHOOK_URL deve ser uma URL HTTP ou HTTPS sem credenciais ou fragmento.");
        }
        this.token = token.trim();
        this.json = json;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(CadastroEvento evento) {
        if (destino == null) return;
        try {
            var payload = new Payload(evento.eventoId(), evento.entidade(), evento.operacao(),
                    evento.registroId(), evento.ocorridoEm());
            var request = HttpRequest.newBuilder(destino)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
            if (!token.isEmpty()) request.header("Authorization", "Bearer " + token);
            client.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, erro) -> {
                        if (erro != null) {
                            log.warn("Webhook {} não entregue (falha de conexão ou timeout).", evento.eventoId());
                        } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            log.warn("Webhook {} rejeitado: HTTP {}.", evento.eventoId(), response.statusCode());
                        }
                    });
        } catch (Exception erro) {
            // O cadastro já foi confirmado; uma falha no envio não altera sua resposta.
            log.warn("Webhook {} não pôde ser enviado ({}).", evento.eventoId(), erro.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public void encerrar() {
        client.shutdownNow();
    }
}
