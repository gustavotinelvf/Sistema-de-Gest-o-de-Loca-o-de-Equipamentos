package br.ufs.ziploc.devolucao.api;

import java.math.BigDecimal;
import java.time.Instant;

public record DevolucaoResumo(
    Long id,
    Long itemContratoId,
    Long usuarioId,
    Instant dataDevolucaoReal,
    BigDecimal multaAtraso,
    String observacoes
) {

}
