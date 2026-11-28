package br.ufs.ziploc.devolucao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarDevolucaoDto(
        @NotNull @Positive Long itemContratoId,
        @Size(max = 500) String observacoes
) {
}