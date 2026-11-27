package br.ufs.ziploc.faturamento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufs.ziploc.faturamento.domain.Fatura;


public interface FaturaRepository extends JpaRepository<Fatura,Long>{
    Optional<Fatura> findByContratoId(Long contratoId);
    Optional<Fatura> findByIdAndLocadoraId(Long id, Long locadoraId);
}
