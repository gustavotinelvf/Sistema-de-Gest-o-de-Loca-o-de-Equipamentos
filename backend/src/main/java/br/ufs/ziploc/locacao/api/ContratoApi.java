package br.ufs.ziploc.locacao.api;

import java.time.LocalDate;
import java.util.Optional;

public interface ContratoApi {
    Optional<ContratoResumo> buscarContrato(Long contratoId);

    Optional<ItemContratoResumo> buscarItemContrato(Long itemContratoId);

    void registrarDevolucao(Long itemContratoId, LocalDate dataDevolucao);

    long contarItensPendentes(Long contratoId);

    void finalizarContrato(Long contratoId);
}
