package br.ufs.ziploc.disponibilidade.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Periodo(LocalDate inicio, LocalDate fim) {
    public Periodo {
        Objects.requireNonNull(inicio, "A data de início é obrigatória.");
        Objects.requireNonNull(fim, "A data de fim é obrigatória.");
        if (!fim.isAfter(inicio)) throw new IllegalArgumentException("A data de fim deve ser posterior à data de início.");
    }

    public boolean sobrepoe(Periodo outro) {
        return !inicio.isAfter(outro.fim) && !outro.inicio.isAfter(fim);
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    public long quantidadeDias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }
}
