package br.ufs.ziploc.locacao.dto;

import java.math.BigDecimal;

public record ItemContratoDTO(Long id, Long unidadeId, BigDecimal valorDiariaContratada) {
}
