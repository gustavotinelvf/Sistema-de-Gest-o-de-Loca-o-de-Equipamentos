package br.ufs.ziploc.faturamento.exception;

import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;

public class FaturaNaoEncontradaException extends RecursoNaoEncontradoException {

    public FaturaNaoEncontradaException(Long faturaId) {
        super("Fatura " + faturaId + " não encontrada.");
    }
}