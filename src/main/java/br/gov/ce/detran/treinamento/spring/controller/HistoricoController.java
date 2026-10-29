package br.gov.ce.detran.treinamento.spring.controller;

import br.gov.ce.detran.treinamento.spring.dto.response.HistoricoResponseDTO;
import br.gov.ce.detran.treinamento.spring.service.HistoricoService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public List<HistoricoResponseDTO> listar() {
        return historicoService.listar();
    }

    @GetMapping("/{id}")
    public HistoricoResponseDTO buscarPorId(@PathVariable @Positive Long id) {
        return historicoService.buscarPorId(id);
    }

}
