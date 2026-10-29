package br.gov.ce.detran.treinamento.spring.repository.specification;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Set;

/** Critérios de consulta da entidade Proprietario, mantidos na camada de persistência. */
public final class ProprietarioSpecifications {
    private static final Set<String> CAMPOS_TEXTO = Set.of(
            "nome", "cpfCnpj", "cidade", "estado", "rua", "cep", "numero", "complemento");

    private ProprietarioSpecifications() {
    }

    public static Specification<Proprietario> consultar(String campo, String valor, String tipoPessoa) {
        return porTipoPessoa(tipoPessoa).and(porCampo(campo, valor));
    }

    static Specification<Proprietario> porTipoPessoa(String tipoPessoa) {
        return (root, query, cb) -> switch (normalizarTipo(tipoPessoa)) {
            case "PF" -> cb.equal(cb.length(root.get("cpfCnpj")), 11);
            case "PJ" -> cb.equal(cb.length(root.get("cpfCnpj")), 14);
            default -> cb.conjunction();
        };
    }

    static Specification<Proprietario> porCampo(String campo, String valor) {
        String campoSeguro = CAMPOS_TEXTO.contains(campo) ? campo : "nome";
        String termo = Normalizacao.texto(valor);
        if (termo == null) return (root, query, cb) -> cb.conjunction();
        if ("cpfCnpj".equals(campoSeguro) || "cep".equals(campoSeguro)) {
            termo = termo.replaceAll("[^A-Z0-9]", "");
        }
        String padrao = "%" + escaparLike(termo) + "%";
        return (root, query, cb) -> cb.like(cb.upper(root.get(campoSeguro)), padrao, '\\');
    }

    private static String normalizarTipo(String tipoPessoa) {
        return tipoPessoa == null ? "" : tipoPessoa.toUpperCase(Locale.ROOT);
    }

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
