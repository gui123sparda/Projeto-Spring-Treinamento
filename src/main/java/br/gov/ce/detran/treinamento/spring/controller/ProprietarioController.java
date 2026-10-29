package br.gov.ce.detran.treinamento.spring.controller;

import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import jakarta.validation.Valid;
import java.util.List;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/proprietarios")
public class ProprietarioController {

    private final ProprietarioService proprietarioService;

    public ProprietarioController(ProprietarioService proprietarioService){
        this.proprietarioService = proprietarioService;
    }

    @GetMapping
    public List<ProprietarioResponseDTO> listar() {
        return proprietarioService.listar();
    }

    @GetMapping("/{id}")
    public ProprietarioResponseDTO buscarPorId(@PathVariable @Positive Integer id){
        return proprietarioService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProprietarioResponseDTO> cadastrar(@RequestBody @Valid ProprietarioRequestDTO dados){
        var proprietario = proprietarioService.cadastrar(dados);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(proprietario.id())
                .toUri();
        return ResponseEntity.created(location).body(proprietario);
    }

    @PatchMapping("/{id}")
    public ProprietarioResponseDTO atualizar(@PathVariable @Positive Integer id,
                                             @RequestBody @Valid ProprietarioAtualizacaoRequestDTO dados){

        return proprietarioService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Integer id) {
        proprietarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

}
