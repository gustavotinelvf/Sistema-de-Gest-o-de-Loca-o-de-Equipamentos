package br.ufs.ziploc.locacao.dto;

import java.time.LocalDate;

/** Corpo opcional da retirada; sem data, vale o dia de hoje. */
public record RetiradaRequest(LocalDate dataRetirada) {
}
