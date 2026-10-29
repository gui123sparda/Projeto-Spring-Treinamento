package br.gov.ce.detran.treinamento.spring.util;

import java.util.Locale;

public final class Normalizacao {
    private Normalizacao() {
    }

    public static String texto(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        return normalizado.isEmpty() ? null : normalizado;
    }

    public static String cep(String valor) {
        String normalizado = texto(valor);
        return normalizado == null ? null : normalizado.replaceAll("[.\\s-]", "");
    }

    /** Preserva campos ausentes no PATCH e rejeita valores explicitamente em branco. */
    public static String campoAtualizacao(String valor) {
        if (valor == null) return null;
        String normalizado = texto(valor);
        if (normalizado == null) {
            throw new IllegalArgumentException("Os campos informados não podem estar em branco.");
        }
        return normalizado;
    }
}
