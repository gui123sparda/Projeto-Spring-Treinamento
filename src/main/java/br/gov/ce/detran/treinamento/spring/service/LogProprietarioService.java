package br.gov.ce.detran.treinamento.spring.service;

import br.gov.ce.detran.treinamento.spring.globalExceptions.exceptions.LogProprietarioNaoEncontradoException;
import br.gov.ce.detran.treinamento.spring.dto.response.LogProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.repository.LogProprietarioRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class LogProprietarioService {

    private final LogProprietarioRepository logProprietarioRepository;

    public LogProprietarioService(LogProprietarioRepository logProprietarioRepository) {
        this.logProprietarioRepository = logProprietarioRepository;
    }

    public List<LogProprietarioResponseDTO> listar() {
        return logProprietarioRepository.findAll().stream()
                .map(LogProprietarioResponseDTO::fromEntity)
                .toList();
    }

    public LogProprietarioResponseDTO buscarPorId(@NotNull @Positive Integer id) {
        var logProprietario = logProprietarioRepository.findById(id)
                .orElseThrow(LogProprietarioNaoEncontradoException::new);
        return LogProprietarioResponseDTO.fromEntity(logProprietario);
    }
}
