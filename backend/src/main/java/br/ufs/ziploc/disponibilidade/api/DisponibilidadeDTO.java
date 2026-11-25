package br.ufs.ziploc.disponibilidade.api;

import java.math.BigDecimal;

public record DisponibilidadeDTO(Long modeloId, String nome, String categoria, BigDecimal valorDiaria, int quantidadeDisponivel) {
}
