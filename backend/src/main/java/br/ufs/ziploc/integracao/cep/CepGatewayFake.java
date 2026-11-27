package br.ufs.ziploc.integracao.cep;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador falso, sem internet. Ativar com ZIPLOC_INTEGRACAO_CEP=fake.
 * Comportamento: "00000000" não existe; "99999999" simula serviço fora do ar;
 * qualquer outro CEP devolve um endereço fixo de Aracaju.
 */
@Component
@ConditionalOnProperty(name = "ziploc.integracao.cep", havingValue = "fake")
public class CepGatewayFake implements CepGateway {

    @Override
    public Optional<EnderecoCep> consultar(String cep) {
        if ("00000000".equals(cep)) {
            return Optional.empty();
        }
        if ("99999999".equals(cep)) {
            throw new CepIndisponivelException();
        }
        return Optional.of(new EnderecoCep(cep, "Rua Exemplo", "Centro", "Aracaju", "SE"));
    }
}
