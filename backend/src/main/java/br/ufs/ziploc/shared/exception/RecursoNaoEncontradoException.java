package br.ufs.ziploc.shared.exception;

/** Recurso inexistente (mapeada para HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
