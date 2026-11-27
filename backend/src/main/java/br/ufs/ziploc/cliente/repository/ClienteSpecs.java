package br.ufs.ziploc.cliente.repository;

import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.CnpjValidador;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;


//Especificações JPA para filtragem dinâmica na listagem de clientes

public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    public static Specification<Cliente> comFiltros(
            Long locadoraId,
            String razaoSocial,
            String cnpj,
            Boolean ativo,
            StatusAdimplencia statusAdimplencia
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (locadoraId != null) {
                predicates.add(cb.equal(root.get("locadoraId"), locadoraId));
            }
            if (razaoSocial != null && !razaoSocial.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("razaoSocial")),
                        "%" + razaoSocial.trim().toLowerCase() + "%"
                ));
            }
            if (cnpj != null && !cnpj.isBlank()) {
                String cnpjLimpo = CnpjValidador.limpar(cnpj);
                if (cnpjLimpo.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.like(root.get("cnpj"), "%" + cnpjLimpo + "%"));
                }
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }
            if (statusAdimplencia != null) {
                predicates.add(cb.equal(root.get("statusAdimplencia"), statusAdimplencia));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}