package br.gov.ce.detran.treinamento.spring.repository;

import br.gov.ce.detran.treinamento.spring.model.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Veiculo> {

    java.util.Optional<Veiculo> findByPlaca(String placa);

    Page<Veiculo> findByIdProprietarioId(Integer idProprietario, Pageable pageable);

    long countByIdProprietarioId(Integer idProprietario);

    long countByTipo(br.gov.ce.detran.treinamento.spring.model.TipoVeiculo tipo);

    default Page<Veiculo> findAllPaginado(Pageable pageable) {
        var pagina = pageable.isPaged() ? pageable.getPageNumber() : 0;
        var tamanho = pageable.isPaged() ? Math.min(pageable.getPageSize(), 15) : 15;
        var ordenacao = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id");
        return findAll(PageRequest.of(pagina, tamanho, ordenacao));
    }
}
