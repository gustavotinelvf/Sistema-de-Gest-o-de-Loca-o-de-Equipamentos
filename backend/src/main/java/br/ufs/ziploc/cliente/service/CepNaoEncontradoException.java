package br.ufs.ziploc.cliente.service;

public class CepNaoEncontradoException extends RuntimeException {

    public CepNaoEncontradoException() {
        super("CEP não encontrado");
    }
}
