package br.ufs.ziploc.integracao.cep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CepGatewayFakeTest {

    private final CepGatewayFake fake = new CepGatewayFake();

    @Test
    void consultar_cepComum_devolveEnderecoFixo() {
        EnderecoCep e = fake.consultar("49010000").orElseThrow();
        assertEquals("49010000", e.cep());
        assertEquals("Aracaju", e.cidade());
    }

    @Test
    void consultar_zeros_naoExiste() {
        assertTrue(fake.consultar("00000000").isEmpty());
    }

    @Test
    void consultar_noves_simulaServicoForaDoAr() {
        assertThrows(CepIndisponivelException.class, () -> fake.consultar("99999999"));
    }
}
