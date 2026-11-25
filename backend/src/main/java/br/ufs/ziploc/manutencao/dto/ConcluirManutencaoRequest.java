package br.ufs.ziploc.manutencao.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ConcluirManutencaoRequest(
        @NotNull(message = "O custo do reparo é obrigatório.")
        @PositiveOrZero(message = "O custo do reparo não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O custo deve ter no máximo duas casas decimais.")
        BigDecimal custoReparo) {
}