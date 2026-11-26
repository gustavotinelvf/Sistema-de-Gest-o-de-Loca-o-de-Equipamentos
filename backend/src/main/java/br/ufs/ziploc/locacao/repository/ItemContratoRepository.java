package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.ItemContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemContratoRepository extends JpaRepository<ItemContrato, Long> {
    @Query(value = """
            SELECT COUNT(*)
            FROM item_contrato i
            WHERE i.contrato_id = :contratoId
            AND NOT EXISTS (
                SELECT 1
                FROM devolucao d
                WHERE d.item_contrato_id = i.id
            )
            """, nativeQuery = true)
    long countByContratoIdAndDataDevolucaoRealIsNull(@Param("contratoId") Long contratoId);
}