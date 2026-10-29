package br.gov.ce.detran.treinamento.spring.util.validation;

import br.gov.ce.detran.treinamento.spring.util.Documento;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // A obrigatoriedade é verificada por @NotBlank.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        try {
            Documento.normalizarCpfCnpj(valor);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
