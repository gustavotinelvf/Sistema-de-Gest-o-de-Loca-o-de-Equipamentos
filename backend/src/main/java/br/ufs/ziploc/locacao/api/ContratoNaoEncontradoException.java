package br.ufs.ziploc.locacao.api;

public class ContratoNaoEncontradoException extends RuntimeException {
    public ContratoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
