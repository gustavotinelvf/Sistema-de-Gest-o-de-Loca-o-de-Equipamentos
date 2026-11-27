package br.ufs.ziploc.cliente.repository;

import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.CnpjValidador;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
        Objects.requireNonNull(locadoraId, "ID da locadora obrigatório.");

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("locadoraId"), locadoraId));

            if (razaoSocial != null && !razaoSocial.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("razaoSocial")),
                        "%" + escaparLike(razaoSocial.trim().toLowerCase(Locale.ROOT)) + "%",
                        '\\'
                ));
            }

            if (cnpj != null && !cnpj.isBlank()) {
                String filtro = cnpj.trim();
                String cnpjLimpo = CnpjValidador.limpar(filtro);

                boolean formatoValido = filtro.matches("[0-9]{1,14}")
                        || filtro.matches("[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2}");

                if (!formatoValido) {
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

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}