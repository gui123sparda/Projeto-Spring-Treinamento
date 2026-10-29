package br.gov.ce.detran.treinamento.spring.controller;

import br.gov.ce.detran.treinamento.spring.dto.response.LogProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.service.LogProprietarioService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/log-proprietarios")
public class LogProprietarioController {

    private final LogProprietarioService logProprietarioService;

    public LogProprietarioController(LogProprietarioService logProprietarioService) {
        this.logProprietarioService = logProprietarioService;
    }

    @GetMapping
    public List<LogProprietarioResponseDTO> listar() {
        return logProprietarioService.listar();
    }

    @GetMapping("/{id}")
    public LogProprietarioResponseDTO buscarPorId(@PathVariable @Positive Integer id) {
        return logProprietarioService.buscarPorId(id);
    }

}
