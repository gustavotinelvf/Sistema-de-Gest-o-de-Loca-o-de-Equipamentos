package br.ufs.ziploc.cliente.repository;

import br.ufs.ziploc.cliente.domain.Cliente;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    boolean existsByContatoEmail(String email);

    boolean existsByContatoEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT c FROM Cliente c
            WHERE c.ativo = false
              AND c.anonimizadoEm IS NULL
              AND c.dataCadastro <= :limite
            """)
    List<Cliente> findInativosParaAnonimizacao(@Param("limite") Instant limite);
}