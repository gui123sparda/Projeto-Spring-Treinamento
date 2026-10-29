package br.gov.ce.detran.treinamento.spring.webhook;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Snapshot independente das entidades JPA, incluindo a identificação exibida na tela. */
public record CadastroEvento(String eventoId, String entidade, String operacao,
                             Integer registroId, String ocorridoEm, String descricao) {
    public static CadastroEvento novo(String entidade, String operacao, Integer registroId) {
        return novo(entidade, operacao, registroId, null);
    }

    public static CadastroEvento novo(String entidade, String operacao, Integer registroId, String descricao) {
        return new CadastroEvento(UUID.randomUUID().toString(), entidade, operacao,
                registroId, OffsetDateTime.now().toString(), descricao);
    }
}
