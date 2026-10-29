package br.gov.ce.detran.treinamento.spring.notificacao;

import br.gov.ce.detran.treinamento.spring.webhook.CadastroEvento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.List;

/** Avisos das alterações feitas pela API, compartilhados pelas telas web desta instância. */
@Component
public class Notificacoes {
    private static final int LIMITE = 200;
    private final ArrayDeque<CadastroEvento> recentes = new ArrayDeque<>();

    public record Consulta(String cursor, List<CadastroEvento> eventos) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public synchronized void registrar(CadastroEvento evento) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) return;
        var request = atributos.getRequest();
        var caminho = request.getRequestURI().substring(request.getContextPath().length());
        if (!caminho.startsWith("/api/")) return;
        recentes.addLast(evento);
        if (recentes.size() > LIMITE) recentes.removeFirst();
    }

    public synchronized Consulta consultar(String apos) {
        var cursor = recentes.isEmpty() ? "inicio" : recentes.getLast().eventoId();
        if (apos == null) return new Consulta(cursor, List.of());
        var eventos = List.copyOf(recentes);
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).eventoId().equals(apos)) {
                return new Consulta(cursor, eventos.subList(i + 1, eventos.size()));
            }
        }
        // Cursor antigo, conexão interrompida ou instância reiniciada: recupera o buffer disponível.
        return new Consulta(cursor, eventos);
    }
}
