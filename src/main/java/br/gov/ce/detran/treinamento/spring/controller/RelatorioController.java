package br.gov.ce.detran.treinamento.spring.controller;

import br.gov.ce.detran.treinamento.spring.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class RelatorioController {
    private final ProprietarioService proprietarios;
    private final VeiculoService veiculos;
    private final HistoricoService historico;
    public RelatorioController(ProprietarioService proprietarios, VeiculoService veiculos, HistoricoService historico) {
        this.proprietarios = proprietarios; this.veiculos = veiculos; this.historico = historico;
    }

    @GetMapping(value = "/relatorio-proprietarios", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> proprietarios(@RequestParam(defaultValue = "nome") String campo,
            @RequestParam(defaultValue = "") String valor, @RequestParam(defaultValue = "") String tipoPessoa) {
        return pdf(proprietarios.relatorioLista(campo, valor, tipoPessoa), "proprietarios.pdf");
    }
    @GetMapping(value = "/relatorio-proprietario", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> proprietario(@RequestParam Integer id) {
        return pdf(proprietarios.relatorioIndividual(id), "proprietario-" + id + ".pdf");
    }
    @GetMapping(value = "/relatorio-veiculos", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> veiculos(@RequestParam(defaultValue = "placa") String campo,
            @RequestParam(defaultValue = "") String valor) {
        return pdf(veiculos.relatorioLista(campo, valor), "veiculos.pdf");
    }
    @GetMapping(value = "/relatorio-veiculo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> veiculo(@RequestParam Integer id) {
        return pdf(veiculos.relatorioIndividual(id), "veiculo-" + id + ".pdf");
    }
    @GetMapping(value = "/relatorio-agrupado", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> agrupado(@RequestParam Integer idProprietario) {
        return pdf(proprietarios.relatorioAgrupado(idProprietario), "proprietario-" + idProprietario + "-veiculos.pdf");
    }
    @GetMapping(value = "/relatorio-historico", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> historico(@RequestParam Map<String, String> filtros) {
        return pdf(historico.relatorio(false, filtros), "historico-proprietarios.pdf");
    }
    @GetMapping(value = "/relatorio-historico-veiculos", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> historicoVeiculos(@RequestParam Map<String, String> filtros) {
        return pdf(historico.relatorio(true, filtros), "historico-veiculos.pdf");
    }
    private ResponseEntity<byte[]> pdf(byte[] bytes, String nome) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(bytes);
    }
}
