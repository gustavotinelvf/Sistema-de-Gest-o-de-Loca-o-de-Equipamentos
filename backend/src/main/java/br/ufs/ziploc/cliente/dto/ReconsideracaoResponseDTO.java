package br.ufs.ziploc.cliente.dto;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;

import java.time.Instant;

public record ReconsideracaoResponseDTO(

        Long id,

        Long clienteId,

        Long gerenteId,

        DecisaoReconsideracao decisao,

        String justificativa,

        Instant dataDecisao,

        Integer cicloAnterior,

        Integer cicloAtual,

        StatusAdimplencia statusAdimplencia

) {
}