package br.gov.ce.detran.treinamento.spring.repository;

import br.gov.ce.detran.treinamento.spring.model.LogProprietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogProprietarioRepository extends JpaRepository<LogProprietario, Integer> {

}
