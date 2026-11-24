package br.ufs.ziploc.equipamento.api;

import java.math.BigDecimal;

/** Visão somente leitura de um modelo, para uso de outros módulos. */
public record ModeloInfo(Long id, String nome, String categoria, BigDecimal valorDiariaBase,
                         int quantidadeMinima, int quantidadeMaxima, boolean ativo) {
}
