package br.ufs.ziploc.equipamento.dto;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;

import java.math.BigDecimal;

public record ModeloEquipamentoResponse(Long id, String nome, String categoria, String descricao, BigDecimal valorDiariaBase,
                                        int quantidadeMinima, Integer quantidadeMaxima, boolean ativo) {
    public static ModeloEquipamentoResponse de(ModeloEquipamento m) {
        return new ModeloEquipamentoResponse(m.getId(), m.getNome(), m.getCategoria(), m.getDescricao(),
                m.getValorDiariaBase(), m.getQuantidadeMinima(), m.getQuantidadeMaxima(), m.isAtivo());
    }
}