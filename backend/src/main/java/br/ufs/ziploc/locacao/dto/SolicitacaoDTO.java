package br.ufs.ziploc.locacao.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitacaoDTO(Long id, Long clienteId, Long registradoPorId, Long analisadoPorId, Instant dataCriacao, Instant dataAnalise, LocalDate dataInicioDesejada, LocalDate dataFimDesejada, String status, String justificativa, List<ItemSolicitacaoDTO> itens) {
}
