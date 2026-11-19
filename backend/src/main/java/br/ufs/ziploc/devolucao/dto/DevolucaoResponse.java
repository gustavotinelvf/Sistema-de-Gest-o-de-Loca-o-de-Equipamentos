package br.ufs.ziploc.devolucao.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.ufs.ziploc.devolucao.domain.Devolucao;

public record DevolucaoResponse(
    Long id,
    Long itemContratoId,
    Instant dataDevolucaoReal,
    String observacoes,
    BigDecimal multaAtraso
) {
    public static DevolucaoResponse recebimentoDevolucao(Devolucao devolucao){
        return new DevolucaoResponse(
            devolucao.getId(),
            devolucao.getItemContratoId(),
            devolucao.getDataDevolucaoReal(),
            devolucao.getObservacoes(),
            devolucao.getMultaAtraso()
        );
    }

}
