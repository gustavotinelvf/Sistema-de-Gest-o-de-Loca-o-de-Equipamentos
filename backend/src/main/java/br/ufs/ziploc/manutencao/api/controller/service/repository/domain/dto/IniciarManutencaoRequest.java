package br.ufs.ziploc.manutencao.dto;

import jakarta.validation.constraints.NotBlank;

public record IniciarManutencaoRequest(
        @NotBlank String numeroSerie,
        @NotBlank String descricaoProblema) { }