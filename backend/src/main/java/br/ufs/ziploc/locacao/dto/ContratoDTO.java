package br.ufs.ziploc.locacao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContratoDTO(Long id, Long solicitacaoId, Long clienteId, Instant dataCriacao, LocalDate dataInicioPrevista, LocalDate dataFimPrevista, String status, BigDecimal valorTotal, BigDecimal percentualMulta, List<ItemContratoDTO> itens) {
}
