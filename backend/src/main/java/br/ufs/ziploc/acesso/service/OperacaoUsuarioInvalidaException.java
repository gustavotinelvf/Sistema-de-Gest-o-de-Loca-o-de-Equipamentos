package br.ufs.ziploc.acesso.service;

/** A operação não é permitida no estado atual (vira 409 Conflict). */
public class OperacaoUsuarioInvalidaException extends RuntimeException {

    public OperacaoUsuarioInvalidaException(String mensagem) {
        super(mensagem);
    }
}
