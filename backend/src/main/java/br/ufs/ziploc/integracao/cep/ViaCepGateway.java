package br.ufs.ziploc.integracao.cep;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador real (ViaCEP). É o padrão; para usar o falso, defina a variável
 * de ambiente ZIPLOC_INTEGRACAO_CEP=fake. Timeout curto: se o serviço
 * demorar, o usuário digita o endereço (seção 4.3 do Relatório).
 */
@Component
@ConditionalOnProperty(name = "ziploc.integracao.cep", havingValue = "real", matchIfMissing = true)
public class ViaCepGateway implements CepGateway {

    private final RestClient restClient;

    @Autowired
    public ViaCepGateway(@Value("${ZIPLOC_CEP_URL:https://viacep.com.br/ws}") String urlBase) {
        this(construir(urlBase));
    }

    /** Usado nos testes, com um RestClient ligado a um servidor simulado. */
    ViaCepGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient construir(String urlBase) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public Optional<EnderecoCep> consultar(String cep) {
        try {
            ViaCepResposta r = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResposta.class);
            if (r == null || Boolean.TRUE.equals(r.erro())) {
                return Optional.empty();
            }
            return Optional.of(new EnderecoCep(cep, r.logradouro(), r.bairro(), r.localidade(), r.uf()));
        } catch (RestClientException e) {
            throw new CepIndisponivelException(e);
        }
    }
}
