package br.gov.ce.detran.treinamento.spring.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import br.gov.ce.detran.treinamento.spring.util.validation.CpfCnpj;
import br.gov.ce.detran.treinamento.spring.util.Documento;
import br.gov.ce.detran.treinamento.spring.util.Normalizacao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@SQLRestriction("excluido_em IS NULL")
@Table(name = "proprietario")
@Getter
@Setter
@NoArgsConstructor
public class Proprietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "ID do proprietário deve ser positivo.")
    private Integer id;

    @NotBlank(message = "Informe o CPF/CNPJ.")
    @CpfCnpj
    @Column(name = "cpf_cnpj", nullable = false, unique = true, updatable = false, length = 14)
    private String cpfCnpj;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 150, message = "Rua deve ter no máximo 150 caracteres.")
    @Column(length = 150)
    private String rua;

    @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
    @Column(length = 20)
    private String numero;

    @Pattern(regexp = "[0-9]{8}", message = "CEP deve conter 8 dígitos.")
    @Column(length = 8)
    private String cep;

    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
    @Column(length = 100)
    private String cidade;

    @Pattern(regexp = "[A-Z]{2}", message = "Estado deve conter a UF com 2 letras.")
    @Column(length = 2)
    private String estado;

    @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres.")
    @Column(length = 255)
    private String complemento;
    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;
    @OneToMany(mappedBy = "idProprietario")
    private Set<Veiculo> veiculos = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    public void normalizarDados() {
        cpfCnpj = Documento.limparCpfCnpj(cpfCnpj);
        nome = Normalizacao.texto(nome);
        rua = Normalizacao.texto(rua);
        numero = Normalizacao.texto(numero);
        cep = Normalizacao.cep(cep);
        cidade = Normalizacao.texto(cidade);
        estado = Normalizacao.texto(estado);
        complemento = Normalizacao.texto(complemento);
    }
}
