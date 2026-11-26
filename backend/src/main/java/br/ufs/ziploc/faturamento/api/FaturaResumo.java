package br.ufs.ziploc.faturamento.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturaResumo(
    Long id,
    Long contratoId,
    BigDecimal valorLocacao,
    BigDecimal valorMulta,
    LocalDate dataVencimento
){

}
