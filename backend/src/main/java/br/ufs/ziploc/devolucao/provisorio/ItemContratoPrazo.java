package br.ufs.ziploc.devolucao.provisorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemContratoPrazo(
    Long contratoId,
    Long unidadeId,
    LocalDate dataFimPrevista,
    BigDecimal valorDiaria,
    BigDecimal percentualMultaAtraso,
    BigDecimal valorLocacao
) {

}
