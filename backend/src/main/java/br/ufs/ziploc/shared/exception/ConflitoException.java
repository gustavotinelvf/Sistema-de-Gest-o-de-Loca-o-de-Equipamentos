package br.ufs.ziploc.shared.exception;

/** Conflito com o estado atual dos dados, como duplicidade ou sobreposição (mapeada para HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
