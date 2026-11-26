package br.ufs.ziploc.equipamento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ModeloEquipamentoRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 100) String categoria,
        @NotNull String descricao,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal valorDiariaBase,
        @NotNull @Min(0) Integer quantidadeMinima,
        @Min(1) Integer quantidadeMaxima) {
}