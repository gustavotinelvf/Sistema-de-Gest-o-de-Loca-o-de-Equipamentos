package br.ufs.ziploc.devolucao.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.ufs.ziploc.devolucao.domain.Devolucao;

public record DevolucaoDto(
    Long id,
    Long itemContratoId,
    Instant dataDevolucaoReal,
    String observacoes,
    BigDecimal multaAtraso
) {
    public static DevolucaoDto recebimentoDevolucao(Devolucao devolucao){
        return new DevolucaoDto(
            devolucao.getId(),
            devolucao.getItemContratoId(),
            devolucao.getDataDevolucaoReal(),
            devolucao.getObservacoes(),
            devolucao.getMultaAtraso()
        );
    }

}
