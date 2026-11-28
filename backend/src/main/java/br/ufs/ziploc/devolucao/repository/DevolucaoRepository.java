package br.ufs.ziploc.devolucao.repository;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {

    Optional<Devolucao> findByItemContratoId(Long itemContratoId);

    List<Devolucao> findByItemContratoIdIn(List<Long> itemContratoIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d FROM Devolucao d
            WHERE d.id = :id AND d.locadoraId = :locadoraId
            """)
    Optional<Devolucao> findByIdAndLocadoraIdForUpdate(
            @Param("id") Long id,
            @Param("locadoraId") Long locadoraId);
}