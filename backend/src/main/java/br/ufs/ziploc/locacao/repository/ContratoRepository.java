package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
}
