package br.ufs.ziploc.cliente.dto;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarReconsideracaoDTO(

        @NotNull
        DecisaoReconsideracao decisao,

        @NotBlank
        @Size(max = 2000)
        String justificativa

) {
}