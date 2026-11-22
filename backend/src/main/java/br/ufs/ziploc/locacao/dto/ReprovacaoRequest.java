package br.ufs.ziploc.locacao.dto;

import jakarta.validation.constraints.NotBlank;

public record ReprovacaoRequest(@NotBlank(message = "A justificativa é obrigatória.") String justificativa) {
}
