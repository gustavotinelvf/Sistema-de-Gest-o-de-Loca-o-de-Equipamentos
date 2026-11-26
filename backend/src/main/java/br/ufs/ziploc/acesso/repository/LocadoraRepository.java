package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Locadora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocadoraRepository extends JpaRepository<Locadora, Long> {

    Optional<Locadora> findFirstByOrderByIdAsc();

    /** O sistema usa uma só locadora: a primeira (e única) linha. */
    default Optional<Locadora> findUnica() {
        return findFirstByOrderByIdAsc();
    }
}
