package br.ufs.ziploc.cliente.service;

public class CepInvalidoException extends RuntimeException {

    public CepInvalidoException() {
        super("CEP inválido: informe 8 dígitos");
    }
}
