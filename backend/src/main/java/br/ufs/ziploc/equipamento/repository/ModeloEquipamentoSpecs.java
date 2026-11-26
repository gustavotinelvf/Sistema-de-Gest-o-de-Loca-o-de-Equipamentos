package br.ufs.ziploc.equipamento.repository;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionais da listagem de modelos; valor nulo/em branco significa "sem filtro". */
public final class ModeloEquipamentoSpecs {
    private ModeloEquipamentoSpecs() {
    }

    public static Specification<ModeloEquipamento> filtrar(String categoria, Boolean ativo) {
        Specification<ModeloEquipamento> spec = (root, query, cb) -> cb.conjunction();
        if (categoria != null && !categoria.isBlank()) {
            String chave = categoria.strip().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("categoria")), chave));
        }
        if (ativo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
        }
        return spec;
    }
}
