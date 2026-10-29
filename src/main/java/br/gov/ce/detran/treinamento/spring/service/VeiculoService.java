package br.gov.ce.detran.treinamento.spring.service;


import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.VeiculoNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.ProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.request.VeiculoAtualizacaoRequestDTO;
import br.gov.ce.detran.treinamento.spring.dto.response.VeiculoResponseDTO;
import br.gov.ce.detran.treinamento.spring.repository.ProprietarioRepository;
import br.gov.ce.detran.treinamento.spring.repository.VeiculoRepository;
import jakarta.validation.Valid;
import br.gov.ce.detran.treinamento.spring.model.TipoVeiculo;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import br.gov.ce.detran.treinamento.spring.webhook.CadastroEvento;
import br.gov.ce.detran.treinamento.spring.util.Relatorios;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final ApplicationEventPublisher eventos;

    public VeiculoService(VeiculoRepository veiculoRepository,
                         ProprietarioRepository proprietarioRepository, ApplicationEventPublisher eventos){
        this.veiculoRepository = veiculoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.eventos = eventos;
    }

    @Transactional
    public VeiculoResponseDTO cadastrar(@NotNull @Valid VeiculoRequestDTO dados){
        var proprietario = proprietarioRepository.findById(dados.idProprietario())
                .orElseThrow(ProprietarioNaoEncontradoException::new);
        var veiculo = veiculoRepository.save(dados.toEntity(proprietario));
        eventos.publishEvent(CadastroEvento.novo("veiculo", "CADASTRO", veiculo.getId(), veiculo.getPlaca()));
        return VeiculoResponseDTO.fromEntity(veiculo);
    }

    public Page<VeiculoResponseDTO> listar(Pageable pageable){
        return veiculoRepository.findAllPaginado(pageable)
                .map(VeiculoResponseDTO::fromEntity);
    }

    public Page<VeiculoResponseDTO> consultar(String campo, String valor, Pageable pageable) {
        return veiculoRepository.findAll(
                br.gov.ce.detran.treinamento.spring.repository.specification.VeiculoSpecifications.consultar(campo, valor), pageable)
                .map(VeiculoResponseDTO::fromEntity);
    }

    public VeiculoResponseDTO buscarPorId(@NotNull @Positive Integer id){
        var veiculo = veiculoRepository.findById(id)
                .orElseThrow(VeiculoNaoEncontradoException::new);

        return VeiculoResponseDTO.fromEntity(veiculo);
    }

    @Transactional
    public VeiculoResponseDTO atualizar(@NotNull @Positive Integer id,
                                        @NotNull @Valid VeiculoAtualizacaoRequestDTO dados) {
        var veiculo = veiculoRepository.findById(id)
                .orElseThrow(VeiculoNaoEncontradoException::new);

        if (dados.idProprietario() != null) {
            var proprietario = proprietarioRepository.findById(dados.idProprietario())
                    .orElseThrow(ProprietarioNaoEncontradoException::new);
            veiculo.setIdProprietario(proprietario);
        }
        if (dados.tipo() != null) veiculo.setTipo(TipoVeiculo.valueOf(dados.tipo()));
        if (dados.anoFabricacao() != null) veiculo.setAnoFabricacao(dados.anoFabricacao());
        if (dados.chassi() != null) veiculo.setChassi(dados.chassi());
        if (dados.renavam() != null) veiculo.setRenavam(dados.renavam());
        if (dados.placa() != null) veiculo.setPlaca(dados.placa());

        var salvo = veiculoRepository.save(veiculo);
        eventos.publishEvent(CadastroEvento.novo("veiculo", "ALTERACAO", salvo.getId(), salvo.getPlaca()));
        return VeiculoResponseDTO.fromEntity(salvo);
    }

    public java.util.Optional<VeiculoResponseDTO> consultarPlaca(String placa) {
        String normalizada = placa == null ? "" : placa.replace("-", "").trim().toUpperCase(java.util.Locale.ROOT);
        return veiculoRepository.findByPlaca(normalizada).map(VeiculoResponseDTO::fromEntity);
    }

    public Page<VeiculoResponseDTO> listarDoProprietario(Integer idProprietario, Pageable pageable) {
        return veiculoRepository.findByIdProprietarioId(idProprietario, pageable)
                .map(VeiculoResponseDTO::fromEntity);
    }

    @Transactional
    public void excluir(@NotNull @Positive Integer id) {
        var veiculo = veiculoRepository.findById(id)
                .orElseThrow(VeiculoNaoEncontradoException::new);
        veiculo.setExcluidoEm(OffsetDateTime.now());
        veiculoRepository.save(veiculo);
        eventos.publishEvent(CadastroEvento.novo("veiculo", "EXCLUSAO_LOGICA", id, veiculo.getPlaca()));
    }

    public byte[] relatorioLista(String campo, String valor) {
        var linhas = veiculoRepository.findAll(
                br.gov.ce.detran.treinamento.spring.repository.specification.VeiculoSpecifications.consultar(campo, valor)).stream()
                .map(v -> {
                    var linha = Relatorios.linhaVeiculo(VeiculoResponseDTO.fromEntity(v));
                    linha.put("proprietario", v.getIdProprietario().getNome());
                    return linha;
                }).toList();
        return Relatorios.gerar("veiculo/relatorio-veiculos", "Lista de veículos", linhas, java.util.Map.of("TOTAL", linhas.size()));
    }

    public byte[] relatorioIndividual(Integer id) {
        var entidade = veiculoRepository.findById(id).orElseThrow(VeiculoNaoEncontradoException::new);
        var v = VeiculoResponseDTO.fromEntity(entidade);
        var p = br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO.fromEntity(entidade.getIdProprietario());
        var linha = Relatorios.linhaProprietario(p);
        linha.putAll(Relatorios.linhaVeiculo(v));
        linha.put("placaImagem", br.gov.ce.detran.treinamento.spring.util.PlacaImagem.renderizar(v.placa(), v.tipo(), p.estado(), p.cidade()));
        if (v.placa().matches("[A-Z]{3}[0-9]{4}")) linha.put("placa", v.placa().substring(0, 3) + "-" + v.placa().substring(3));
        return Relatorios.gerar("veiculo/relatorio-veiculo", "Consulta do veículo", java.util.List.of(linha), java.util.Map.of());
    }
}
