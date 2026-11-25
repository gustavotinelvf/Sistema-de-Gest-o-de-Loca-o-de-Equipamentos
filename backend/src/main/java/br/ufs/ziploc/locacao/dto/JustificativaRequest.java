package br.ufs.ziploc.locacao.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do cancelamento de solicitação ou de contrato (RD012: justificativa obrigatória). */
public record JustificativaRequest(@NotBlank String justificativa) {
}
