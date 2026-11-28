package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    List<Contrato> findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(
            Long locadoraId);

    List<Contrato> findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(
            Long locadoraId, StatusContrato status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contrato c WHERE c.id = :id")
    Optional<Contrato> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Contrato c
            WHERE c.locadoraId = :locadoraId
            AND EXISTS (
                SELECT i.id FROM ItemContrato i
                WHERE i.contrato = c
                AND i.id = :itemContratoId
            )
            """)
    Optional<Contrato> findByItemContratoIdAndLocadoraIdForUpdate(
            @Param("itemContratoId") Long itemContratoId,
            @Param("locadoraId") Long locadoraId);
}