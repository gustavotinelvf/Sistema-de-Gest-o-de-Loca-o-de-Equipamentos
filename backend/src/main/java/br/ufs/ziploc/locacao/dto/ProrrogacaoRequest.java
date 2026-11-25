package br.ufs.ziploc.locacao.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProrrogacaoRequest(@NotNull LocalDate novaDataFim) {
}
