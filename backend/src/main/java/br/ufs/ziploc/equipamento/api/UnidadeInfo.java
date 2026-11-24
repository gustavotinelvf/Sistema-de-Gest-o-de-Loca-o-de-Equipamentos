package br.ufs.ziploc.equipamento.api;

import java.math.BigDecimal;

/**
 * Visão somente leitura de uma unidade física, para uso de outros módulos.
 * O status vai como texto (DISPONIVEL, LOCADA, EM_VISTORIA, EM_MANUTENCAO) para que ninguém importe o domínio de equipamento.
 */
public record UnidadeInfo(Long id, Long modeloId, String numeroSerie, String status, boolean ativo,
                          BigDecimal valorDiariaBase) {

    /** Unidade ativa e livre, apta a ser retirada. */
    public boolean disponivel() {
        return ativo && "DISPONIVEL".equals(status);
    }
}
