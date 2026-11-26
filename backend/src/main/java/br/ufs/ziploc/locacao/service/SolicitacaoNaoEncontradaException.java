package br.ufs.ziploc.locacao.service;

public class SolicitacaoNaoEncontradaException extends RuntimeException {
    public SolicitacaoNaoEncontradaException(Long id) {
        super("Solicitação " + id + " não encontrada.");
    }
}
