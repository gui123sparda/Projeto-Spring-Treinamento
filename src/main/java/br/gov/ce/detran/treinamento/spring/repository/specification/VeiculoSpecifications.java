package br.gov.ce.detran.treinamento.spring.repository.specification;

import br.gov.ce.detran.treinamento.spring.model.*;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;
import org.springframework.data.jpa.domain.Specification;
import java.util.Set;

public final class VeiculoSpecifications {
    private VeiculoSpecifications() {}
    public static Specification<Veiculo> consultar(String campo, String valor) {
        String termo = Normalizacao.texto(valor);
        if (termo == null) return (root, query, cb) -> cb.conjunction();
        String seguro = Set.of("placa", "renavam", "chassi", "tipo", "anoFabricacao", "proprietario").contains(campo) ? campo : "placa";
        if ("placa".equals(seguro)) termo = termo.replace("-", "");
        String padrao = "%" + termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            if ("tipo".equals(seguro)) {
                try { return cb.equal(root.get("tipo"), TipoVeiculo.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT))); }
                catch (IllegalArgumentException e) { return cb.disjunction(); }
            }
            if ("anoFabricacao".equals(seguro)) {
                try { return cb.equal(root.get("anoFabricacao"), Integer.parseInt(valor.trim())); }
                catch (NumberFormatException e) { return cb.disjunction(); }
            }
            var coluna = "proprietario".equals(seguro) ? root.get("idProprietario").<String>get("nome") : root.<String>get(seguro);
            return cb.like(cb.upper(coluna), padrao, '\\');
        };
    }
}
