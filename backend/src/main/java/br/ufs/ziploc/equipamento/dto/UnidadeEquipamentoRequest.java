package br.ufs.ziploc.equipamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnidadeEquipamentoRequest(
        @NotNull Long modeloId,
        @NotBlank @Size(max = 50) String numeroSerie) {
}
