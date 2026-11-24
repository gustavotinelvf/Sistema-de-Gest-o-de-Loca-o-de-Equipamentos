package br.ufs.ziploc.locacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record RegistrarSolicitacaoRequest(
        @NotNull Long clienteId,
        // TODO: remover quando a autenticação existir; passar a vir do usuário logado.
        @NotNull Long registradoPorId,
        @NotNull LocalDate dataInicioDesejada,
        @NotNull LocalDate dataFimDesejada,
        @NotEmpty @Valid List<Item> itens) {

    public record Item(@NotNull Long modeloId, @NotNull @Positive Integer quantidade) {
    }
}
