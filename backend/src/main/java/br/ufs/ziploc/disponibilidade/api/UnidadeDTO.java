package br.ufs.ziploc.disponibilidade.api;

import java.math.BigDecimal;

public record UnidadeDTO(Long unidadeId, Long modeloId, BigDecimal valorDiaria) {
}
