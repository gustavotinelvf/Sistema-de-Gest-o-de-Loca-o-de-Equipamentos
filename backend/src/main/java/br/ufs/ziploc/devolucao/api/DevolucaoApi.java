package br.ufs.ziploc.devolucao.api;

import java.util.Optional;

public interface DevolucaoApi {
    Optional<DevolucaoResumo> buscarPorId(Long id);
    Optional<DevolucaoResumo> buscarPorItemContratoId(Long itemContratoId);
}
