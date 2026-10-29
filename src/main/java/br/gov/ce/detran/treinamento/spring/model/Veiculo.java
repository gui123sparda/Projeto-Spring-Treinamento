package br.gov.ce.detran.treinamento.spring.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@SQLRestriction("excluido_em IS NULL")
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {
    @ColumnDefault("'CARRO'")
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_veiculo")
    private TipoVeiculo tipo;
    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "id_proprietario", nullable = false)
    private Proprietario idProprietario;
    @NotNull
    @Column(name = "ano_fabricacao", nullable = false)
    private Integer anoFabricacao;
    @Size(max = 17)
    @NotNull
    @Column(name = "chassi", nullable = false, length = 17)
    private String chassi;
    @Size(max = 11)
    @NotNull
    @Column(name = "renavam", nullable = false, length = 11)
    private String renavam;
    @Size(max = 7)
    @NotNull
    @Column(name = "placa", nullable = false, length = 7)
    private String placa;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "ID do veiculo deve ser positivo.")
    private Integer id;


}
