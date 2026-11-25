package br.ufs.ziploc.manutencao.api;

/**
 * Porta de entrada do módulo manutenção para os demais módulos.
 * Participa da transação do chamador.
 */
public interface ManutencaoApi {

    /**
     * Abre uma manutenção CORRETIVA para a unidade, a partir de uma vistoria reprovada.
     * Idempotente: se a unidade já tem manutenção aberta, devolve a existente.
     * Quem muda o status da unidade para EM_MANUTENCAO é a Vistoria.
     */
    ManutencaoResumo abrirAPartirDeVistoria(Long locadoraId, Long unidadeId,
                                            Long tecnicoId, String descricaoAvarias);
}