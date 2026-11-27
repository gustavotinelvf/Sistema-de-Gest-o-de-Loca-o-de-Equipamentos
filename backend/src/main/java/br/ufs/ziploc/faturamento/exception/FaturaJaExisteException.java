package br.ufs.ziploc.faturamento.exception;

import br.ufs.ziploc.shared.exception.ConflitoException;

public class FaturaJaExisteException extends ConflitoException {

    public FaturaJaExisteException(Long contratoId) {
        super("Já existe fatura para o contrato " + contratoId + ".");
    }
}