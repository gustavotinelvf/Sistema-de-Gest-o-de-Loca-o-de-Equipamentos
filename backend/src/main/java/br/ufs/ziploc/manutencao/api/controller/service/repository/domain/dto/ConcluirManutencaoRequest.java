package br.ufs.ziploc.manutencao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ConcluirManutencaoRequest(
        @NotNull @PositiveOrZero BigDecimal custoReparo) { }