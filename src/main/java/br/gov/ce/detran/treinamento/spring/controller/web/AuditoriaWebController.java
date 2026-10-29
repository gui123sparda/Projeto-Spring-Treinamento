package br.gov.ce.detran.treinamento.spring.controller.web;
import br.gov.ce.detran.treinamento.spring.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class AuditoriaWebController {
    private final HistoricoService historico;
    private final LogProprietarioService logs;
    public AuditoriaWebController(HistoricoService historico, LogProprietarioService logs) {
        this.historico = historico; this.logs = logs;
    }
    @GetMapping({"/historico", "/historico-veiculos"})
    public String historico(jakarta.servlet.http.HttpServletRequest request,
            @RequestParam java.util.Map<String, String> filtros,
            @RequestParam(defaultValue = "0") int page, Model model) {
        boolean veiculos = request.getRequestURI().equals(request.getContextPath() + "/historico-veiculos");
        model.addAttribute("pagina", historico.consultar(veiculos, filtros,
                org.springframework.data.domain.PageRequest.of(Math.max(0, page), 15)));
        model.addAttribute("filtros", filtros);
        model.addAttribute("historicoVeiculos", veiculos);
        model.addAttribute("caminhoHistorico", veiculos ? "/historico-veiculos" : "/historico");
        model.addAttribute("caminhoRelatorio", veiculos ? "/relatorio-historico-veiculos" : "/relatorio-historico");
        return "historico/lista";
    }
    @GetMapping("/historico/{id}")
    public String historicoDetalhes(@PathVariable Long id, Model model) {
        model.addAttribute("registro", historico.buscarPorId(id)); return "historico/detalhes";
    }
    @GetMapping("/log-proprietarios")
    public String logs(Model model) {
        model.addAttribute("registros", logs.listar()); return "log-proprietarios/lista";
    }
    @GetMapping("/log-proprietarios/{id}")
    public String logDetalhes(@PathVariable Integer id, Model model) {
        model.addAttribute("registro", logs.buscarPorId(id)); return "log-proprietarios/detalhes";
    }
}
