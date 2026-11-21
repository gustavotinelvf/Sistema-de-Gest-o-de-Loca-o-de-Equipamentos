package br.ufs.ziploc.equipamento.api;

import java.util.Optional;

public interface EquipamentoApi {
    Optional<Long> buscarUnidadeIdPorNumeroSerie(String numeroSerie);
}

// TODO: NOMES A SEREM SUBSTITUÍDOS A POSTERIORI, NOMES TEMPORÁRIOS PARA VER SE TÁ FUNCIONANDO NUM TESTE