package br.ufs.ziploc.manutencao.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConcluirManutencaoRequest(
        @NotNull(message = "O custo gerado é obrigatório.")
        @PositiveOrZero(message = "O custo gerado não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "O custo deve ter no máximo duas casas decimais.")
        BigDecimal custoGerado,
        @Size(max = 2000) String justificativaAtraso) {
}