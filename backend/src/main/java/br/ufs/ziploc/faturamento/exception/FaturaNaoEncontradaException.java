package br.ufs.ziploc.faturamento.exception;

public class FaturaNaoEncontradaException extends RuntimeException {

    public FaturaNaoEncontradaException(Long faturaId) {
        super("Fatura " + faturaId + " não encontrada.");
    }
}
