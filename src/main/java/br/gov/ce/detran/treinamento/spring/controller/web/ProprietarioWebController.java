package br.gov.ce.detran.treinamento.spring.controller.web;

import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioForm;
import br.gov.ce.detran.treinamento.spring.service.ProprietarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proprietarios")
public class ProprietarioWebController {
    private final ProprietarioService proprietarioService;

    public ProprietarioWebController(ProprietarioService proprietarioService) {
        this.proprietarioService = proprietarioService;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "nome") String campo,
                         @RequestParam(defaultValue = "") String valor,
                         @RequestParam(defaultValue = "") String tipoPessoa,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        int pagina = Math.max(0, page);
        var pageable = PageRequest.of(pagina, 10);
        model.addAttribute("pagina", proprietarioService.consultar(campo, valor, tipoPessoa, pageable));
        model.addAttribute("campo", campo);
        model.addAttribute("valor", valor);
        model.addAttribute("tipoPessoa", tipoPessoa);
        return "proprietarios/lista";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(defaultValue = "PF") String tipoPessoa,
                       @RequestParam(defaultValue = "") String origem, Model model) {
        model.addAttribute("proprietario", new ProprietarioForm());
        model.addAttribute("tipoPessoa", tipoPessoa);
        model.addAttribute("edicao", false);
        model.addAttribute("origem", origem);
        return "proprietarios/formulario";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("proprietario") ProprietarioForm form,
                            BindingResult binding, @RequestParam(defaultValue = "PF") String tipoPessoa,
                            @RequestParam(defaultValue = "") String origem, Model model, RedirectAttributes redirect) {
        model.addAttribute("origem", origem);
        if (binding.hasErrors()) return formularioComErro(model, tipoPessoa, false, null);
        var salvo = proprietarioService.cadastrar(form.paraCadastro());
        if ("veiculo".equals(origem)) {
            redirect.addFlashAttribute("mensagem", "Proprietário cadastrado. Continue o cadastro do veículo.");
            return "redirect:/veiculos/novo?idProprietario=" + salvo.id();
        }
        redirect.addFlashAttribute("mensagem", "Proprietário cadastrado com sucesso.");
        return "redirect:/proprietarios?tipoPessoa=" + tipoPessoa;
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Integer id, Model model) {
        model.addAttribute("proprietario", proprietarioService.buscarPorId(id));
        model.addAttribute("veiculos", proprietarioService.listarVeiculos(id,
                PageRequest.of(0, 3, org.springframework.data.domain.Sort.by("id"))));
        return "proprietarios/detalhes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        var dados = proprietarioService.buscarPorId(id);
        model.addAttribute("proprietario", ProprietarioForm.from(dados));
        model.addAttribute("id", id);
        model.addAttribute("tipoPessoa", dados.cpfCnpj().length() == 14 ? "PJ" : "PF");
        model.addAttribute("edicao", true);
        return "proprietarios/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id,
                            @Valid @ModelAttribute("proprietario") ProprietarioForm form,
                            BindingResult binding, Model model, RedirectAttributes redirect) {
        String tipo = form.getCpfCnpj() != null && form.getCpfCnpj().replaceAll("[^A-Za-z0-9]", "").length() == 14 ? "PJ" : "PF";
        if (binding.hasErrors()) return formularioComErro(model, tipo, true, id);
        proprietarioService.atualizar(id, form.paraAtualizacao());
        redirect.addFlashAttribute("mensagem", "Proprietário atualizado com sucesso.");
        return "redirect:/proprietarios?tipoPessoa=" + tipo;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        proprietarioService.excluir(id);
        redirect.addFlashAttribute("mensagem", "Proprietário excluído com sucesso.");
        return "redirect:/proprietarios";
    }

    private static String formularioComErro(Model model, String tipoPessoa, boolean edicao, Integer id) {
        model.addAttribute("tipoPessoa", tipoPessoa);
        model.addAttribute("edicao", edicao);
        if (id != null) model.addAttribute("id", id);
        return "proprietarios/formulario";
    }
}
