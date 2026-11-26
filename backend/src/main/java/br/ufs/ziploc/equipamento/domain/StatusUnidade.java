package br.ufs.ziploc.equipamento.domain;

import java.util.EnumSet;
import java.util.Set;

public enum StatusUnidade {
    DISPONIVEL, LOCADA, EM_MANUTENCAO, EM_VISTORIA, INDISPONIVEL;

    public boolean podeTransicionarPara(StatusUnidade destino) {
        return destinosPermitidos().contains(destino);
    }

    private Set<StatusUnidade> destinosPermitidos() {
        return switch (this) {
            case DISPONIVEL -> EnumSet.of(LOCADA, EM_MANUTENCAO);
            case LOCADA -> EnumSet.of(EM_VISTORIA);
            case EM_VISTORIA -> EnumSet.of(DISPONIVEL, EM_MANUTENCAO);
            case EM_MANUTENCAO -> EnumSet.of(DISPONIVEL);
            case INDISPONIVEL -> EnumSet.noneOf(StatusUnidade.class);
        };
    }
}