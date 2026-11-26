package br.ufs.ziploc.locacao.api;

import java.util.Optional;

public interface ContratoApi {
    Optional<ContratoResumo> buscarContrato(Long contratoId);

    Optional<ItemContratoPrazo> buscarItemContrato(Long itemContratoId);

    long contarItensPendentes(Long contratoId);

    void finalizarContrato(Long contratoId);
}