package br.ufs.ziploc.cliente.repository;

import br.ufs.ziploc.cliente.domain.Cliente;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByCnpj(String cnpj);

    Optional<Cliente> findByCnpjAndLocadoraId(String cnpj, Long locadoraId);

    boolean existsByCnpj(String cnpj);

    Optional<Cliente> findByIdAndLocadoraId(Long id, Long locadoraId);

    Page<Cliente> findAllByLocadoraId(Long locadoraId, Pageable pageable);

    @Query("""
            SELECT c FROM Cliente c
            WHERE c.ativo = false
              AND c.dataAnonimizacao IS NULL
              AND c.dataCadastro <= :limite
            """)
    List<Cliente> findInativosParaAnonimizacao(@Param("limite") Instant limite);
}