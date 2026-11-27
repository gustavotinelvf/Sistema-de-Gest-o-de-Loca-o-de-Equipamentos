package br.ufs.ziploc.acesso.domain;

public enum StatusUsuario {
    /** Criado pelo convite, ainda sem senha. Só vira ATIVO quando aceita o convite. */
    PENDENTE,
    ATIVO,
    INATIVO
}
