package br.ufs.ziploc.devolucao.provisorio;

import java.time.Instant;

public record ItemContratoPrazo(
    Long contratoId,
    Long unidadeId,
    Instant dataFimPrevista
) {

}
