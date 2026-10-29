package br.gov.ce.detran.treinamento.spring.service;

import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.HistoricoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.dto.response.HistoricoResponseDTO;
import br.gov.ce.detran.treinamento.spring.repository.HistoricoRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import br.gov.ce.detran.treinamento.spring.util.Relatorios;

@Service
@Validated
@Transactional(readOnly = true)
public class HistoricoService {

    private final HistoricoRepository historicoRepository;
    private final JdbcClient jdbc;

    public HistoricoService(HistoricoRepository historicoRepository, JdbcClient jdbc) {
        this.historicoRepository = historicoRepository;
        this.jdbc = jdbc;
    }

    public List<HistoricoResponseDTO> listar() {
        return historicoRepository.findAll().stream()
                .map(HistoricoResponseDTO::fromEntity)
                .toList();
    }

    public HistoricoResponseDTO buscarPorId(@NotNull @Positive Long id) {
        var historico = historicoRepository.findById(id)
                .orElseThrow(HistoricoNaoEncontradoException::new);
        return HistoricoResponseDTO.fromEntity(historico);
    }
    public Page<Map<String, Object>> consultar(boolean veiculos, Map<String, String> filtros, Pageable pageable) {
        var parametros = new HashMap<String, Object>();
        parametros.put("entidade", veiculos ? "veiculo" : "proprietario");
        var where = new StringBuilder(" WHERE entidade = :entidade");
        String id = filtros.getOrDefault("registroId", "");
        if (!id.isBlank()) {
            try {
                long numero = Long.parseLong(id);
                if (numero <= 0) throw new NumberFormatException();
                parametros.put("registroId", numero);
            } catch (NumberFormatException e) { throw new IllegalArgumentException("ID do cadastro inválido."); }
            where.append(" AND registro_id = :registroId");
        }
        String operacao = filtros.getOrDefault("operacao", "");
        if (!operacao.isBlank()) {
            if (!Set.of("LEGADO", "MARCO_INICIAL", "CADASTRO", "ALTERACAO", "EXCLUSAO_LOGICA").contains(operacao))
                throw new IllegalArgumentException("Operação de histórico inválida.");
            where.append(" AND operacao = :operacao"); parametros.put("operacao", operacao);
        }
        LocalDate de = data(filtros.getOrDefault("dataDe", ""));
        LocalDate ate = data(filtros.getOrDefault("dataAte", ""));
        if (de != null && ate != null && de.isAfter(ate)) throw new IllegalArgumentException("A data inicial deve ser anterior à data final.");
        var zona = ZoneId.of("America/Fortaleza");
        if (de != null) {
            where.append(" AND ocorrido_em >= :de"); parametros.put("de", de.atStartOfDay(zona).toOffsetDateTime());
        }
        if (ate != null) {
            where.append(" AND ocorrido_em < :ate"); parametros.put("ate", ate.plusDays(1).atStartOfDay(zona).toOffsetDateTime());
        }
        String busca = filtros.getOrDefault("busca", "").trim();
        if (!busca.isEmpty()) {
            where.append(" AND position(treino.normalizar_busca(:busca) in treino.normalizar_busca(coalesce(antes::text,'') || coalesce(depois::text,''))) > 0");
            parametros.put("busca", busca);
        }
        long total = jdbc.sql("SELECT count(*) FROM treino.historico" + where).params(parametros).query(Long.class).single();
        String sql = """
                SELECT id::text AS id, entidade, registro_id::text AS "registroId", operacao,
                    to_char(ocorrido_em AT TIME ZONE 'America/Fortaleza','DD/MM/YYYY HH24:MI:SS') AS data,
                    usuario_banco AS usuario, coalesce(jsonb_pretty(antes),'—') AS antes,
                    coalesce(jsonb_pretty(depois),'—') AS depois
                FROM treino.historico
                """ + where + " ORDER BY ocorrido_em DESC, id DESC";
        if (pageable.isPaged()) {
            sql += " LIMIT :limite OFFSET :offset";
            parametros.put("limite", pageable.getPageSize()); parametros.put("offset", pageable.getOffset());
        }
        var linhas = jdbc.sql(sql).params(parametros).query((rs, row) -> {
            Map<String, Object> linha = new LinkedHashMap<>();
            for (String campo : List.of("id", "entidade", "registroId", "operacao", "data", "usuario", "antes", "depois"))
                linha.put(campo, rs.getString(campo));
            return linha;
        }).list();
        return new PageImpl<>(linhas, pageable, total);
    }
    private LocalDate data(String valor) {
        try { return valor.isBlank() ? null : LocalDate.parse(valor); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Data inválida. Informe AAAA-MM-DD."); }
    }

    public byte[] relatorio(boolean deVeiculos, Map<String, String> filtros) {
        var linhas = consultar(deVeiculos, filtros, Pageable.unpaged()).getContent().stream().map(original -> {
            Map<String, Object> linha = new HashMap<>(original);
            linha.put("antes", Relatorios.ocultarIds((String) linha.get("antes")));
            linha.put("depois", Relatorios.ocultarIds((String) linha.get("depois")));
            return linha;
        }).toList();
        return Relatorios.gerar("historico", deVeiculos ? "Histórico de veículos" : "Histórico de proprietários", linhas, Map.of());
    }
}
