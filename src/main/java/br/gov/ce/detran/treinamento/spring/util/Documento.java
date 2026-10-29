package br.gov.ce.detran.treinamento.spring.util;

import java.util.Locale;

/** Validação de CPF/CNPJ adaptada do projeto TreinamentoCRUDServlet. */
public final class Documento {

    private Documento() {
    }

    public static String limparCpfCnpj(String valor) {
        return valor == null ? "" : valor.toUpperCase(Locale.ROOT).replaceAll("[.\\s/-]", "");
    }

    public static String normalizarCpfCnpj(String valor) {
        String documento = limparCpfCnpj(valor);
        if (!documento.matches("[0-9]{11}")
                && !documento.matches("[A-Z0-9]{12}[0-9]{2}")) {
            throw new IllegalArgumentException("Informe CPF com 11 dígitos ou CNPJ com 12 caracteres alfanuméricos e 2 dígitos finais.");
        }
        if (documento.length() == 11) {
            if (documento.matches("([0-9])\\1{10}")
                    || documento.charAt(9) - '0' != digitoCpf(documento.substring(0, 9))
                    || documento.charAt(10) - '0' != digitoCpf(documento.substring(0, 10))) {
                throw new IllegalArgumentException("CPF inválido: confira os dígitos verificadores.");
            }
        } else if (documento.matches("([0-9])\\1{13}")
                || documento.charAt(12) - '0' != digitoCnpj(documento.substring(0, 12))
                || documento.charAt(13) - '0' != digitoCnpj(documento.substring(0, 13))) {
            throw new IllegalArgumentException("CNPJ inválido: confira os dígitos verificadores.");
        }
        return documento;
    }

    private static int digitoCpf(String base) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (base.length() + 1 - i);
        }
        int resultado = 11 - soma % 11;
        return resultado >= 10 ? 0 : resultado;
    }

    /** Receita Federal: valor ASCII - 48, pesos 2 a 9 da direita para a esquerda e módulo 11. */
    private static int digitoCnpj(String base) {
        int soma = 0;
        for (int i = base.length() - 1; i >= 0; i--) {
            int peso = 2 + (base.length() - 1 - i) % 8;
            soma += (base.charAt(i) - 48) * peso;
        }
        int resultado = 11 - soma % 11;
        return resultado >= 10 ? 0 : resultado;
    }

}
