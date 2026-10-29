package br.gov.ce.detran.treinamento.spring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "historico")
public class Historico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "entidade", nullable = false, length = Integer.MAX_VALUE)
    private String entidade;

    @NotNull
    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @NotNull
    @Column(name = "operacao", nullable = false, length = Integer.MAX_VALUE)
    private String operacao;

    @NotNull
    @ColumnDefault("clock_timestamp()")
    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    @NotNull
    @ColumnDefault("SESSION_USER")
    @Column(name = "usuario_banco", nullable = false, length = Integer.MAX_VALUE)
    private String usuarioBanco;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "antes")
    private Map<String, Object> antes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depois")
    private Map<String, Object> depois;


}