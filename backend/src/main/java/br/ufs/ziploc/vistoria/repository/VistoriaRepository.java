package br.ufs.ziploc.vistoria.repository;

import br.ufs.ziploc.vistoria.domain.Vistoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    Optional<Vistoria> findByDevolucaoId(Long devolucaoId);
}