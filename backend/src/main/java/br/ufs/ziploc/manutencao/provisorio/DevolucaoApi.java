package br.ufs.ziploc.manutencao.provisorio;

import java.math.BigDecimal;

/**
 * TODO: PROVISÓRIO. Combinado com o colega da Devolução: ele vai criar
 * registrarDanos(Long unidadeId, BigDecimal valor) na DevolucaoApi oficial (devolucao/api).
 * Quando a branch dele chegar: apagar esta pasta (manutencao/provisorio) e trocar o import
 * br.ufs.ziploc.manutencao.provisorio.DevolucaoApi por br.ufs.ziploc.devolucao.api.DevolucaoApi
 * no ManutencaoService e no ManutencaoServiceTest.
 */
public interface DevolucaoApi {

    /** Repassa à Devolução o custo de reparo de uma manutenção originada de vistoria. */
    void registrarDanos(Long unidadeId, BigDecimal valor);
}