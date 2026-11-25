package br.ufs.ziploc.manutencao.api;

import java.time.Instant;

public record ManutencaoResumo(Long id, Long unidadeId, Long vistoriaId, String status, Instant dataInicio) {
}