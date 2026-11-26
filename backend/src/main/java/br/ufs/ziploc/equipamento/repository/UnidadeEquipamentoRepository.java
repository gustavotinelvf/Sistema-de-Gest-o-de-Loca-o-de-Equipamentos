package br.ufs.ziploc.equipamento.repository;

import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UnidadeEquipamentoRepository
        extends JpaRepository<UnidadeEquipamento, Long>, JpaSpecificationExecutor<UnidadeEquipamento> {

    /** RD009: o número de série é único; a checagem ignora maiúsculas/minúsculas para evitar "ab-1" e "AB-1". */
    boolean existsByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByNumeroSerieIgnoreCaseAndIdNot(String numeroSerie, Long id);

    Optional<UnidadeEquipamento> findByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByModeloIdAndStatus(Long modeloId, StatusUnidade status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UnidadeEquipamento u WHERE u.id = :id")
    Optional<UnidadeEquipamento> findByIdForUpdate(@Param("id") Long id);
}