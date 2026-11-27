package br.ufs.ziploc.acesso.service;

public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado: " + id);
    }
}
