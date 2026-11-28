package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByStatus(StatusContrato status, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contrato c WHERE c.id = :id")
    Optional<Contrato> findByIdForUpdate(@Param("id") Long id);
}