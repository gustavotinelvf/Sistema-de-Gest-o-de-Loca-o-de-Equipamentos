package br.ufs.ziploc.acesso.domain;

/**
 * Perfis de acesso. Gravado como texto na coluna "perfil" (nunca como número).
 * RESPONSAVEL existe no CHECK do banco (V1 do Michael) e não está na lista de
 * cinco perfis do Relatório: precisa ser confirmado pelo Tech Lead.
 */
public enum Perfil {
    RESPONSAVEL(true),
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
