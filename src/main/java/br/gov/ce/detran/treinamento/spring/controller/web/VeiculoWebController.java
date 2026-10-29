package br.gov.ce.detran.treinamento.spring.controller.web;

import br.gov.ce.detran.treinamento.spring.dto.request.*;
import br.gov.ce.detran.treinamento.spring.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/veiculos")
public class VeiculoWebController {
    private final VeiculoService veiculos;
    private final ProprietarioService proprietarios;
    public VeiculoWebController(VeiculoService veiculos, ProprietarioService proprietarios) {
        this.veiculos = veiculos;
        this.proprietarios = proprietarios;
    }
    @GetMapping
    public String listar(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "placa") String campo,
                         @RequestParam(defaultValue = "") String valor, Model model) {
        model.addAttribute("pagina", veiculos.consultar(campo, valor, PageRequest.of(Math.max(0, page), 15, org.springframework.data.domain.Sort.by("id"))));
        model.addAttribute("nomesProprietarios", proprietarios.listar().stream().collect(
                java.util.stream.Collectors.toMap(p -> p.id(), p -> p.nome())));
        model.addAttribute("campo", campo);
        model.addAttribute("valor", valor);
        return "veiculos/lista";
    }
    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Integer idProprietario, Model model) {
        var form = new VeiculoForm();
        if (idProprietario != null) {
            proprietarios.buscarPorId(idProprietario);
            form.setIdProprietario(idProprietario);
        }
        model.addAttribute("veiculo", form);
        return formulario(model, null);
    }
    @GetMapping("/consultar-placa")
    public String consultarPlaca(@RequestParam(defaultValue = "") String placa, Model model) {
        model.addAttribute("placaPesquisada", placa);
        if (!placa.isBlank()) {
            var resultado = veiculos.consultarPlaca(placa);
            if (resultado.isPresent()) {
                var veiculo = resultado.get();
                model.addAttribute("veiculo", veiculo);
                model.addAttribute("proprietario", proprietarios.buscarPorId(veiculo.idProprietario()));
            } else {
                model.addAttribute("aviso", "Nenhum veículo encontrado para a placa informada.");
            }
        }
        return "veiculos/consulta-placa";
    }
    @GetMapping(value = "/{id}/placa.png", produces = "image/png")
    @ResponseBody
    public byte[] imagemPlaca(@PathVariable Integer id) throws java.io.IOException {
        var veiculo = veiculos.buscarPorId(id);
        var proprietario = proprietarios.buscarPorId(veiculo.idProprietario());
        if (!veiculo.placa().matches("[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Placa inválida para visualização.");
        }
        var imagem = br.gov.ce.detran.treinamento.spring.util.PlacaImagem.renderizar(
                veiculo.placa(), veiculo.tipo(), proprietario.estado(), proprietario.cidade());
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagem, "png", bytes);
        return bytes.toByteArray();
    }
    @GetMapping("/{id}")
    public String detalhes(@PathVariable Integer id, Model model) {
        var veiculo = veiculos.buscarPorId(id);
        model.addAttribute("veiculo", veiculo);
        model.addAttribute("proprietario", proprietarios.buscarPorId(veiculo.idProprietario()));
        return "veiculos/detalhes";
    }
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("veiculo", VeiculoForm.from(veiculos.buscarPorId(id)));
        return formulario(model, id);
    }
    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("veiculo") VeiculoForm form, BindingResult binding,
                            Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formulario(model, null);
        veiculos.cadastrar(form.paraCadastro());
        redirect.addFlashAttribute("mensagem", "Veículo cadastrado com sucesso.");
        return "redirect:/veiculos";
    }
    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id, @Valid @ModelAttribute("veiculo") VeiculoForm form,
                            BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formulario(model, id);
        veiculos.atualizar(id, form.paraAtualizacao());
        redirect.addFlashAttribute("mensagem", "Veículo atualizado com sucesso.");
        return "redirect:/veiculos";
    }
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, @RequestParam(required = false) Integer idProprietarioRetorno, RedirectAttributes redirect) {
        if (idProprietarioRetorno != null) proprietarios.buscarPorId(idProprietarioRetorno);
        veiculos.excluir(id);
        redirect.addFlashAttribute("mensagem", "Veículo excluído com sucesso.");
        return idProprietarioRetorno == null ? "redirect:/veiculos"
                : "redirect:/proprietarios/" + idProprietarioRetorno + "/veiculos";
    }
    private String formulario(Model model, Integer id) {
        model.addAttribute("id", id);
        model.addAttribute("edicao", id != null);
        model.addAttribute("proprietarios", proprietarios.listar());
        return "veiculos/formulario";
    }
}
