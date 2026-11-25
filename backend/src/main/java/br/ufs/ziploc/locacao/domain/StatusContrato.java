package br.ufs.ziploc.locacao.domain;

public enum StatusContrato {
    PLANEJADO, ATIVO, FINALIZADO, CANCELADO;

    public boolean podeTransicionarPara(StatusContrato destino) {
        return switch (this) {
            case PLANEJADO -> destino == ATIVO || destino == CANCELADO;
            case ATIVO -> destino == FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
    }
}
