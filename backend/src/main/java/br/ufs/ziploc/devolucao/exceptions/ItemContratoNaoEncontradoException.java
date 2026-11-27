package br.ufs.ziploc.devolucao.exceptions;

import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;

public class ItemContratoNaoEncontradoException extends RecursoNaoEncontradoException {
    
    public ItemContratoNaoEncontradoException(Long itemContratoId) {
        super("Item de contrato " + itemContratoId + " não encontrado");
    }
}
