package br.gov.ce.detran.treinamento.spring.controller.web;

import br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository;
import br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository;
import br.gov.ce.detran.treinamento.spring.model.TipoVeiculo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class HomeController {
    private final ProprietarioRepository proprietarios;
    private final VeiculoRepository veiculos;
    private final JdbcClient jdbc;

    public HomeController(ProprietarioRepository proprietarios, VeiculoRepository veiculos, JdbcClient jdbc) {
        this.proprietarios = proprietarios;
        this.veiculos = veiculos;
        this.jdbc = jdbc;
    }

    public record ResumoDiario(LocalDate dia, long cadastros, long alteracoes) {
        public String getRotulo() { return dia.format(DateTimeFormatter.ofPattern("dd/MM")); }
    }

    @GetMapping("/")
    public String inicio(@RequestParam(defaultValue = "") String tipoPessoa, Model model) {
        String filtro = "PF".equals(tipoPessoa) || "PJ".equals(tipoPessoa) ? tipoPessoa : "";
        long carros = veiculos.countByTipo(TipoVeiculo.CARRO);
        long motos = veiculos.countByTipo(TipoVeiculo.MOTO);
        long total = carros + motos;
        var historico = jdbc.sql("""
                SELECT dias.dia,
                    (SELECT COUNT(DISTINCT l.id_proprietario) FROM treino.log_proprietario l
                     WHERE l.data_criacao >= dias.dia AND l.data_criacao < dias.dia + 1
                       AND (:tipo = '' OR (:tipo = 'PF' AND LENGTH(l.cpf_cnpj) = 11)
                                       OR (:tipo = 'PJ' AND LENGTH(l.cpf_cnpj) = 14))) AS cadastros,
                    (SELECT COUNT(DISTINCT l.id_proprietario) FROM treino.log_proprietario l
                     WHERE l.data_alteracao >= dias.dia AND l.data_alteracao < dias.dia + 1
                       AND (:tipo = '' OR (:tipo = 'PF' AND LENGTH(l.cpf_cnpj) = 11)
                                       OR (:tipo = 'PJ' AND LENGTH(l.cpf_cnpj) = 14))) AS alteracoes
                FROM (SELECT CURRENT_DATE - numero AS dia FROM generate_series(0, 13) AS serie(numero)) dias
                ORDER BY dias.dia
                """).param("tipo", filtro).query((rs, row) -> new ResumoDiario(
                        rs.getDate("dia").toLocalDate(), rs.getLong("cadastros"), rs.getLong("alteracoes"))).list();
        model.addAttribute("totalVeiculos", total);
        model.addAttribute("totalCarros", carros);
        model.addAttribute("totalMotos", motos);
        model.addAttribute("totalProprietarios", proprietarios.count());
        model.addAttribute("percentualCarros", total == 0 ? 0 : Math.round(carros * 100.0 / total));
        model.addAttribute("historicoProprietarios", historico);
        model.addAttribute("escalaHistorico", Math.max(1, historico.stream()
                .mapToLong(dia -> Math.max(dia.cadastros(), dia.alteracoes())).max().orElse(1)));
        model.addAttribute("tipoPessoaHistorico", filtro);
        return "index";
    }
}
