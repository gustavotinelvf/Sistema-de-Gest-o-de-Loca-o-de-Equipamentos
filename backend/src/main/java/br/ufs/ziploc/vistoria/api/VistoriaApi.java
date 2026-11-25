package br.ufs.ziploc.vistoria.api;

import java.math.BigDecimal;

/**
 * Porta de entrada do módulo vistoria para os demais módulos.
 * Itens ainda sem vistoria contam como zero de dano.
 */
public interface VistoriaApi {

    /** Custo de dano apurado na vistoria da devolução deste item do contrato (0 se não houver). */
    BigDecimal totalizarDanosDoItem(Long itemContratoId);

    /** Soma dos custos de dano das vistorias de todos os itens do contrato. */
    BigDecimal totalizarDanosDoContrato(Long contratoId);
}