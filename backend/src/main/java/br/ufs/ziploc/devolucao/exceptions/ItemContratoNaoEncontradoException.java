package br.ufs.ziploc.devolucao.exceptions;

public class ItemContratoNaoEncontradoException extends RuntimeException {
    
    public ItemContratoNaoEncontradoException(Long itemContratoId) {
        super("Item de contrato " + itemContratoId + " não encontrado");
    }
}
