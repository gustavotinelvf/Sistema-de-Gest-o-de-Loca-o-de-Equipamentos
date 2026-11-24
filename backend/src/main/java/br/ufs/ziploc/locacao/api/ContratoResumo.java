package br.ufs.ziploc.locacao.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratoResumo(Long id, Long clienteId, String status, LocalDate dataInicioPrevista, LocalDate dataFimPrevista, BigDecimal valorTotal, BigDecimal percentualMulta, List<ItemContratoPrazo> itens) {
}
