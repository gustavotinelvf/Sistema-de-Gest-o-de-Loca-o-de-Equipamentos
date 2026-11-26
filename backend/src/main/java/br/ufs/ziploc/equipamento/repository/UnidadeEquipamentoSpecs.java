package br.ufs.ziploc.equipamento.repository;

import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionais da listagem de unidades; valor nulo/em branco significa "sem filtro". */
public final class UnidadeEquipamentoSpecs {
    private UnidadeEquipamentoSpecs() {
    }

    public static Specification<UnidadeEquipamento> filtrar(Long modeloId, String categoria, StatusUnidade status, Boolean ativo) {
        Specification<UnidadeEquipamento> spec = (root, query, cb) -> cb.conjunction();
        if (modeloId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("modelo").get("id"), modeloId));
        }
        if (categoria != null && !categoria.isBlank()) {
            String chave = categoria.strip().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("modelo").get("categoria")), chave));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (ativo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
        }
        return spec;
    }
}
