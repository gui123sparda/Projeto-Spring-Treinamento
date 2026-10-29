package br.gov.ce.detran.treinamento.spring.service;

import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.ProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.ProprietarioAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository;
import br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository;
import br.gov.ce.detran.treinamento.spring.dto.response.VeiculoResponseDTO;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import br.gov.ce.detran.treinamento.spring.webhook.CadastroEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import br.gov.ce.detran.treinamento.spring.util.Relatorios;
import java.time.OffsetDateTime;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.RequisicaoInvalidaException;
import br.gov.ce.detran.treinamento.spring.repository.specification.ProprietarioSpecifications;

/** Exemplo de camada de serviço para operações de proprietário. */
@Service
@Validated
@Transactional(readOnly = true)
public class ProprietarioService {

    private final ProprietarioRepository proprietarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final ApplicationEventPublisher eventos;

    public ProprietarioService(ProprietarioRepository proprietarioRepository,
            VeiculoRepository veiculoRepository, ApplicationEventPublisher eventos) {
        this.proprietarioRepository = proprietarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.eventos = eventos;
    }

    @Transactional
    public ProprietarioResponseDTO cadastrar(@NotNull @Valid ProprietarioRequestDTO dados) {
        var proprietario = proprietarioRepository.save(dados.toEntity());
        eventos.publishEvent(CadastroEvento.novo("proprietario", "CADASTRO", proprietario.getId(), proprietario.getNome()));
        return ProprietarioResponseDTO.fromEntity(proprietario);
    }

    public List<ProprietarioResponseDTO> listar() {
        return proprietarioRepository.findAll().stream()
                .map(ProprietarioResponseDTO::fromEntity)
                .toList();
    }

    public Page<ProprietarioResponseDTO> consultar(String campo, String valor,
                                                    String tipoPessoa, Pageable pageable) {
        return proprietarioRepository
                .findAll(ProprietarioSpecifications.consultar(campo, valor, tipoPessoa), pageable)
                .map(ProprietarioResponseDTO::fromEntity);
    }

    public ProprietarioResponseDTO buscarPorId(@NotNull @Positive Integer id) {
        var proprietario = proprietarioRepository.findById(id)
                .orElseThrow(ProprietarioNaoEncontradoException::new);
        return ProprietarioResponseDTO.fromEntity(proprietario);
    }

    @Transactional
    public ProprietarioResponseDTO atualizar(@NotNull @Positive Integer id,
                                             @NotNull @Valid ProprietarioAtualizacaoRequestDTO dados) {
        var proprietario = proprietarioRepository.findById(id)
                .orElseThrow(ProprietarioNaoEncontradoException::new);

        if (dados.cpfCnpj() != null && !dados.cpfCnpj().equals(proprietario.getCpfCnpj())) {
            throw new RequisicaoInvalidaException("CPF/CNPJ não pode ser alterado após o cadastro.");
        }
        if (dados.nome() != null) proprietario.setNome(dados.nome());
        if (dados.rua() != null) proprietario.setRua(dados.rua());
        if (dados.numero() != null) proprietario.setNumero(dados.numero());
        if (dados.cep() != null) proprietario.setCep(dados.cep());
        if (dados.cidade() != null) proprietario.setCidade(dados.cidade());
        if (dados.estado() != null) proprietario.setEstado(dados.estado());
        if (dados.complemento() != null) proprietario.setComplemento(dados.complemento());

        var salvo = proprietarioRepository.save(proprietario);
        eventos.publishEvent(CadastroEvento.novo("proprietario", "ALTERACAO", salvo.getId(), salvo.getNome()));
        return ProprietarioResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public void excluir(@NotNull @Positive Integer id) {
        var proprietario = proprietarioRepository.findById(id)
                .orElseThrow(ProprietarioNaoEncontradoException::new);
        proprietario.setExcluidoEm(OffsetDateTime.now());
        proprietarioRepository.save(proprietario);
        eventos.publishEvent(CadastroEvento.novo("proprietario", "EXCLUSAO_LOGICA", id, proprietario.getNome()));
    }

    public Page<VeiculoResponseDTO> listarVeiculos(Integer id, Pageable pageable) {
        return veiculoRepository.findByIdProprietarioId(id, pageable).map(VeiculoResponseDTO::fromEntity);
    }

    public byte[] relatorioLista(String campo, String valor, String tipoPessoa) {
        var linhas = consultar(campo, valor, tipoPessoa, Pageable.unpaged()).stream()
                .map(Relatorios::linhaProprietario).toList();
        String titulo = "PJ".equals(tipoPessoa) ? "Lista de empresas"
                : "PF".equals(tipoPessoa) ? "Lista de pessoas físicas" : "Lista de proprietários";
        return Relatorios.gerar("proprietario/relatorio-proprietarios", titulo, linhas, Map.of("TOTAL", linhas.size()));
    }

    public byte[] relatorioIndividual(Integer id) {
        var entidade = proprietarioRepository.findById(id).orElseThrow(ProprietarioNaoEncontradoException::new);
        var p = ProprietarioResponseDTO.fromEntity(entidade);
        boolean empresa = p.cpfCnpj().length() == 14;
        return Relatorios.gerar("proprietario/relatorio-proprietario", empresa ? "Relatório da empresa" : "Relatório do proprietário",
                List.of(Relatorios.linhaProprietario(p)), Map.of("TIPO", empresa ? "Empresa" : "Pessoa física",
                        "DOCUMENTO_LABEL", empresa ? "CNPJ" : "CPF", "TOTAL_VEICULOS", Math.toIntExact(veiculoRepository.countByIdProprietarioId(id))));
    }

    public byte[] relatorioAgrupado(Integer id) {
        var entidade = proprietarioRepository.findById(id).orElseThrow(ProprietarioNaoEncontradoException::new);
        var p = ProprietarioResponseDTO.fromEntity(entidade);
        List<Map<String, ?>> linhas = new ArrayList<>();
        var veiculos = listarVeiculos(id, Pageable.unpaged(Sort.by("id"))).getContent();
        if (veiculos.isEmpty()) linhas.add(Relatorios.linhaAgrupada(p, null));
        else veiculos.forEach(v -> linhas.add(Relatorios.linhaAgrupada(p, v)));
        return Relatorios.gerar("agrupado", "Proprietário e seus veículos", linhas, Map.of());
    }
}
