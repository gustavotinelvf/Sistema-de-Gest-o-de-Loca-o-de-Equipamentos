package br.ufs.ziploc.locacao.repository;

import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {
    List<Solicitacao> findByLocadoraIdOrderByDataCriacaoAscIdAsc(Long locadoraId);

    List<Solicitacao> findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(Long locadoraId, StatusSolicitacao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitacao s where s.id = :id")
    Optional<Solicitacao> findComTravaById(Long id);
}