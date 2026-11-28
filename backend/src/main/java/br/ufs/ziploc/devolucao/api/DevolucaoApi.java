package br.ufs.ziploc.devolucao.api;

import java.util.Optional;

public interface DevolucaoApi {

    Optional<DevolucaoResumo> buscarPorId(Long id);

    Optional<DevolucaoResumo> buscarPorItemContratoId(Long itemContratoId);

    /** Consulta a devolução da locadora com bloqueio até o fim da transação. */
    Optional<DevolucaoResumo> buscarPorIdParaAtualizacao(Long id, Long locadoraId);
}