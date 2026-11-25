package br.ufs.ziploc.equipamento.repository;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModeloEquipamentoRepository
        extends JpaRepository<ModeloEquipamento, Long>, JpaSpecificationExecutor<ModeloEquipamento> {

    @Query("select distinct m.categoria from ModeloEquipamento m order by m.categoria")
    List<String> listarCategorias();
}
