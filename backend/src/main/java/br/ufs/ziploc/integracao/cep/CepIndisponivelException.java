package br.ufs.ziploc.integracao.cep;

/** O serviço de CEP falhou (fora do ar, lento ou resposta inválida). O usuário digita o endereço. */
public class CepIndisponivelException extends RuntimeException {

    public CepIndisponivelException(Throwable causa) {
        super("Consulta de CEP indisponível no momento", causa);
    }

    public CepIndisponivelException() {
        super("Consulta de CEP indisponível no momento");
    }
}
