package br.ufs.ziploc.devolucao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufs.ziploc.devolucao.domain.Devolucao;


public interface DevolucaoRepository extends JpaRepository<Devolucao,Long> {

    Optional<Devolucao> findByItemContratoId(Long itemContratoId);
}
