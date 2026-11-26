package br.ufs.ziploc.faturamento.api;

import java.math.BigDecimal;

public interface FaturaApi {
    
    FaturaResumo gerarFatura(
        Long contratoId,
        Long locadoraId, 
        Long usuarioId, 
        BigDecimal valorMulta
    ); 

}
