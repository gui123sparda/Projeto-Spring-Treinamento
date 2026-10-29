package br.gov.ce.detran.treinamento.spring.controller;

import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.response.VeiculoResponseDTO;
import br.gov.ce.detran.treinamento.spring.service.VeiculoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    public Page<VeiculoResponseDTO> listar(
            @PageableDefault(size = 15, sort = "id") Pageable pageable) {
        return veiculoService.listar(pageable);
    }

    @GetMapping("/{id}")
    public VeiculoResponseDTO buscarPorId(@PathVariable @Positive Integer id) {
        return veiculoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@RequestBody @Valid VeiculoRequestDTO dados) {
        var veiculo = veiculoService.cadastrar(dados);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(veiculo.id())
                .toUri();
        return ResponseEntity.created(location).body(veiculo);
    }

    @PatchMapping("/{id}")
    public VeiculoResponseDTO atualizar(@PathVariable @Positive Integer id,
                                        @RequestBody @Valid VeiculoAtualizacaoRequestDTO dados) {
        return veiculoService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Integer id) {
        veiculoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
