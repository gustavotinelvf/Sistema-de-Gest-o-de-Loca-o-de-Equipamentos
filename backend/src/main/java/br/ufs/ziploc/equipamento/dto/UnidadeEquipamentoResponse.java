package br.ufs.ziploc.equipamento.dto;

import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;

public record UnidadeEquipamentoResponse(Long id, Long modeloId, String modeloNome, String categoria, String numeroSerie,
                                         StatusUnidade status, boolean ativo) {
    /** Deve ser chamado dentro da transação (o modelo é carregado sob demanda). */
    public static UnidadeEquipamentoResponse de(UnidadeEquipamento u) {
        return new UnidadeEquipamentoResponse(u.getId(), u.getModelo().getId(), u.getModelo().getNome(),
                u.getModelo().getCategoria(), u.getNumeroSerie(), u.getStatus(), u.isAtivo());
    }
}
