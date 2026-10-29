package br.gov.ce.detran.treinamento.spring.controller.web;

import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import br.gov.ce.detran.treinamento.spring.service.VeiculoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProprietarioVeiculosWebController {
    private final ProprietarioService proprietarios;
    private final VeiculoService veiculos;

    public ProprietarioVeiculosWebController(ProprietarioService proprietarios, VeiculoService veiculos) {
        this.proprietarios = proprietarios;
        this.veiculos = veiculos;
    }

    @GetMapping("/proprietarios/{id}/veiculos")
    public String listar(@PathVariable Integer id, @RequestParam(defaultValue = "0") int page, Model model) {
        var proprietario = proprietarios.buscarPorId(id);
        model.addAttribute("proprietario", proprietario);
        model.addAttribute("pagina", veiculos.listarDoProprietario(id,
                PageRequest.of(Math.max(0, page), 15, Sort.by("id"))));
        model.addAttribute("nomesProprietarios", java.util.Map.of(id, proprietario.nome()));
        return "proprietarios/veiculos";
    }
}
