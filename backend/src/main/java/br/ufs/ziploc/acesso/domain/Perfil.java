package br.ufs.ziploc.acesso.domain;

/**
 * Perfis de acesso 
 * Gravado como texto na coluna "perfil" (nunca como número).
 */
public enum Perfil {
    GERENTE(true),
    FINANCEIRO(true),
    ATENDENTE(false),
    ESTOQUISTA(false),
    TECNICO(false);

    private final boolean grupoAdministrativo;

    Perfil(boolean grupoAdministrativo) {
        this.grupoAdministrativo = grupoAdministrativo;
    }

    public boolean isGrupoAdministrativo() {
        return grupoAdministrativo;
    }
}
