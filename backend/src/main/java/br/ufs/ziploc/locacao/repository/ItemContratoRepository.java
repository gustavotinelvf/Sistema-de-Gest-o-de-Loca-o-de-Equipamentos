package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.ItemContrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemContratoRepository extends JpaRepository<ItemContrato, Long> {
    long countByContratoIdAndDataDevolucaoRealIsNull(Long contratoId);
}
