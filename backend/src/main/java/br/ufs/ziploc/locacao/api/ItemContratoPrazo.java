package br.ufs.ziploc.locacao.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemContratoPrazo(Long id, Long contratoId, Long clienteId, Long unidadeId, LocalDate dataInicioPrevista, LocalDate dataFimPrevista, LocalDate dataInicioReal, LocalDate dataDevolucaoReal, BigDecimal valorDiariaContratada, BigDecimal percentualMulta, BigDecimal valorLocacao) {
}
