package br.ufs.ziploc.integracao.cep;

import java.util.Optional;

/**
 * Porta de saída para a consulta de CEP (D7: serviço externo só por integracao/).
 * Dois adaptadores: ViaCepGateway (real) e CepGatewayFake (testes e desenvolvimento sem internet).
 */
public interface CepGateway {

    /**
     * @param cep oito dígitos, sem traço
     * @return o endereço, ou vazio se o CEP não existir
     * @throws CepIndisponivelException se o serviço externo falhar
     */
    Optional<EnderecoCep> consultar(String cep);
}
