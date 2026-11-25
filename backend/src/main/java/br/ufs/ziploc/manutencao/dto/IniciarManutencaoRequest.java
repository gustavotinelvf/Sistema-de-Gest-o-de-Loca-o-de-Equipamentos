package br.ufs.ziploc.manutencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IniciarManutencaoRequest(
        @NotBlank(message = "O número de série é obrigatório.") @Size(max = 50) String numeroSerie,
        @NotBlank(message = "A descrição do problema é obrigatória.") @Size(max = 2000) String descricaoProblema) {
}