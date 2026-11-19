package br.ufs.ziploc.devolucao.provisorio;

import java.time.Instant;
import java.util.Optional;

/* TODO: É PROVISÓRIO ATÉ TER O CONTRATO API DE FATO */
public interface ContratoApi {
    
    Optional<ItemContratoPrazo> buscarItemContrato(Long itemContratoId);
    void registrarDevolucao(Long itemContratoId, Instant dataDevolucao);
}
