package br.ufs.ziploc.faturamento.exception;

public class FaturaJaExisteException extends RuntimeException {

     public FaturaJaExisteException(Long contratoId) {
        super("Já existe fatura para o contrato " + contratoId + ".");
    }
}
