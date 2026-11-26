package br.ufs.ziploc.equipamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarNumeroSerieRequest(@NotBlank @Size(max = 50) String numeroSerie) {
}
