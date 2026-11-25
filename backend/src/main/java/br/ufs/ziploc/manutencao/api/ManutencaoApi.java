package br.ufs.ziploc.manutencao.api;

/**
 * Porta de entrada do módulo manutenção para os demais módulos.
 * Participa da transação do chamador.
 */
public interface ManutencaoApi {

    /**
     * Abre a manutenção de uma unidade a partir de uma vistoria reprovada. Idempotente: se a vistoria já
     * tem manutenção, devolve a existente. Quem muda o status da unidade para EM_MANUTENCAO é a Vistoria.
     */
    ManutencaoResumo abrirAPartirDeVistoria(Long vistoriaId, Long unidadeId,
                                            Long usuarioTecnicoId, String descricaoProblema);
}