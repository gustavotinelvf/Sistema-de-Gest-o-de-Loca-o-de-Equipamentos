package br.ufs.ziploc.devolucao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarDevolucaoDto(
    @NotNull Long itemContratoId,
    @NotNull Long clienteId,
    @Size (max = 500) String observacoes
) {
}
