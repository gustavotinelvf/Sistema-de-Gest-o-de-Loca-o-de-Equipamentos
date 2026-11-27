package br.ufs.ziploc.integracao.cep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ViaCepGatewayTest {

    private MockRestServiceServer servidor;
    private ViaCepGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.com.br/ws");
        servidor = MockRestServiceServer.bindTo(builder).build();
        gateway = new ViaCepGateway(builder.build());
    }

    @AfterEach
    void verificarRequisicoes() {
        servidor.verify();
    }

    @Test
    void consultar_cepExistente_devolveEndereco() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess("""
                        {"cep":"49010-000","logradouro":"Rua A","complemento":"","bairro":"Centro",
                         "localidade":"Aracaju","uf":"SE","ibge":"2800308"}
                        """, MediaType.APPLICATION_JSON));

        EnderecoCep e = gateway.consultar("49010000").orElseThrow();

        assertEquals("49010000", e.cep());
        assertEquals("Rua A", e.logradouro());
        assertEquals("Centro", e.bairro());
        assertEquals("Aracaju", e.cidade());
        assertEquals("SE", e.uf());
    }

    @Test
    void consultar_cepInexistente_devolveVazio() {
        servidor.expect(requestTo("https://viacep.com.br/ws/00000000/json/"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertTrue(gateway.consultar("00000000").isEmpty());
    }

    @Test
    void consultar_servicoComErro_lancaCepIndisponivel() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withServerError());

        assertThrows(CepIndisponivelException.class, () -> gateway.consultar("49010000"));
    }

    @Test
    void consultar_respostaVazia_lancaCepIndisponivel() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(CepIndisponivelException.class, () -> gateway.consultar("49010000"));
    }

    @Test
    void consultar_jsonInvalido_lancaCepIndisponivel() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess("{json-invalido", MediaType.APPLICATION_JSON));

        assertThrows(CepIndisponivelException.class, () -> gateway.consultar("49010000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"uf\":\"SE\"}",
            "{\"localidade\":\"Aracaju\"}",
            "{\"localidade\":\"\",\"uf\":\"SE\"}",
            "{\"localidade\":\" \",\"uf\":\"SE\"}",
            "{\"localidade\":\"Aracaju\",\"uf\":\"S\"}",
            "{\"localidade\":\"Aracaju\",\"uf\":\"123\"}"
    })
    void consultar_respostaIncompletaOuInvalida_lancaCepIndisponivel(String resposta) {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess(resposta, MediaType.APPLICATION_JSON));

        assertThrows(CepIndisponivelException.class, () -> gateway.consultar("49010000"));
    }

    @Test
    void consultar_cepGenericoSemLogradouroEBairro_devolveEndereco() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess("""
                        {"logradouro":"","bairro":"","localidade":"Aracaju","uf":"SE"}
                        """, MediaType.APPLICATION_JSON));

        EnderecoCep e = gateway.consultar("49010000").orElseThrow();

        assertEquals("", e.logradouro());
        assertEquals("", e.bairro());
        assertEquals("Aracaju", e.cidade());
        assertEquals("SE", e.uf());
    }

    @Test
    void consultar_camposOpcionaisNulos_normalizaParaVazio() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(withSuccess("""
                        {"logradouro":null,"bairro":null,"localidade":"Aracaju","uf":"SE"}
                        """, MediaType.APPLICATION_JSON));

        EnderecoCep e = gateway.consultar("49010000").orElseThrow();

        assertEquals("", e.logradouro());
        assertEquals("", e.bairro());
    }

    @Test
    void consultar_timeout_lancaCepIndisponivel() {
        servidor.expect(requestTo("https://viacep.com.br/ws/49010000/json/"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Timeout simulado");
                });

        assertThrows(CepIndisponivelException.class, () -> gateway.consultar("49010000"));
    }
}
