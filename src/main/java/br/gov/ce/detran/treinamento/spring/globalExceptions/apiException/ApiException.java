package br.gov.ce.detran.treinamento.spring.globalExceptions.apiException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Corpo padronizado das respostas de erro dos controllers. */
public record ApiException(
        String dataHora,
        int status,
        String excecao,
        String mensagem,
        List<Map<String, String>> erros) {

    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR"));

    public static ApiException criar(int status, Exception excecao, String mensagem,
                                    List<Map<String, String>> erros) {
        return new ApiException(ZonedDateTime.now(FUSO).format(FORMATO), status,
                excecao.getClass().getSimpleName(), mensagem, List.copyOf(erros));
    }
}
