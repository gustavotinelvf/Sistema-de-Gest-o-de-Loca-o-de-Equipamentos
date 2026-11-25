package br.ufs.ziploc.vistoria.dto;

import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarVistoriaRequest(
        @NotNull(message = "O resultado da vistoria é obrigatório.") ResultadoVistoria resultado,
        @Size(max = 2000) String descricaoAvarias) {
}