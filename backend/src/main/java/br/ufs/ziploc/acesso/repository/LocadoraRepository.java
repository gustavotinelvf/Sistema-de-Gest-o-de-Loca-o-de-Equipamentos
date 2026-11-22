package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Locadora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocadoraRepository extends JpaRepository<Locadora, Long> {

    /** A locadora é uma linha única (id = 1). */
    default Optional<Locadora> findUnica() {
        return findById(Locadora.ID_UNICO);
    }
}
