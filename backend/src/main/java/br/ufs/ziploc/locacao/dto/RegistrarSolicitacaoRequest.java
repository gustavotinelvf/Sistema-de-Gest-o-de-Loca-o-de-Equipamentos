package br.ufs.ziploc.locacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record RegistrarSolicitacaoRequest(
        @NotNull Long clienteId,
        @NotNull LocalDate dataInicioDesejada,
        @NotNull LocalDate dataFimDesejada,
        @NotEmpty @Valid List<Item> itens) {

    public record Item(@NotNull Long modeloId, @NotNull @Positive Integer quantidade) {
    }
}
