package br.ufs.ziploc.acesso.service;

/** Usuário inexistente, inativo ou senha errada: sempre a mesma mensagem genérica. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos");
    }
}
