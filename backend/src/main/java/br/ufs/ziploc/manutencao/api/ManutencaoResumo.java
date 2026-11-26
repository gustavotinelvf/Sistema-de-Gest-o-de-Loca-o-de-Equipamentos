package br.ufs.ziploc.manutencao.api;

import java.time.Instant;

public record ManutencaoResumo(Long id, Long unidadeId, String tipo, Instant dataInicio) {
}