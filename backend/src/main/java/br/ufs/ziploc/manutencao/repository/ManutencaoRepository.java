package br.ufs.ziploc.manutencao.repository;

import br.ufs.ziploc.manutencao.domain.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findByUnidadeIdOrderByDataInicioDesc(Long unidadeId);

    /** Manutenção ainda aberta (sem data de conclusão) da unidade. */
    Optional<Manutencao> findFirstByUnidadeIdAndDataConclusaoIsNull(Long unidadeId);
}