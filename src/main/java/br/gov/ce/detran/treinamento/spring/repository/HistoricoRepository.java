package br.gov.ce.detran.treinamento.spring.repository;

import br.gov.ce.detran.treinamento.spring.model.Historico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoRepository extends JpaRepository<Historico, Long> {

}
