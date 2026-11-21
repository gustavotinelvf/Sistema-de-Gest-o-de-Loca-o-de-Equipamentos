package br.ufs.ziploc.manutencao.repository;

import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.StatusManutencao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findByUnidadeIdOrderByDataInicioDesc(Long unidadeId);

    boolean existsByUnidadeIdAndStatusIn(Long unidadeId, Collection<StatusManutencao> status);

    Optional<Manutencao> findByVistoriaId(Long vistoriaId);
}

// O Spring Data gera as consultas a partir do NOME DOS MÉTODOS! Aqui não se escreve o SQL.