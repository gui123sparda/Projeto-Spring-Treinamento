package br.gov.ce.detran.treinamento.spring.repository;

import br.gov.ce.detran.treinamento.spring.model.Proprietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProprietarioRepository extends JpaRepository<Proprietario, Integer>,
        JpaSpecificationExecutor<Proprietario> {

}
