package br.ufs.ziploc.cliente.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CnpjValidadorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "11.222.333/0001-81",
            "04252011000110",
            "04.252.011/0001-10"
    })
    void deveAceitarCnpjsValidosComOuSemMascara_RD007(String cnpj) {
        assertTrue(CnpjValidador.ehValido(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "123456789",
            "11111111111111",
            "00000000000000",
            "11222333000182",
            "11222333000191",
            "112223330001810"
    })
    void deveRejeitarCnpjsInvalidos_RD007(String cnpj) {
        assertFalse(CnpjValidador.ehValido(cnpj));
    }

    @Test
    void deveRejeitarCnpjNulo_RD007() {
        assertFalse(CnpjValidador.ehValido(null));
    }
}