package br.ufs.ziploc.cliente.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.cliente.dto.CepResponseDTO;
import br.ufs.ziploc.integracao.cep.CepGateway;
import br.ufs.ziploc.integracao.cep.CepIndisponivelException;
import br.ufs.ziploc.integracao.cep.EnderecoCep;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultaCepServiceTest {

    @Mock
    private CepGateway cepGateway;

    @InjectMocks
    private ConsultaCepService service;

    @Test
    void consultar_cepComTraco_normalizaEDevolveEndereco() {
        when(cepGateway.consultar("49010000")).thenReturn(
                Optional.of(new EnderecoCep("49010000", "Rua A", "Centro", "Aracaju", "SE")));

        CepResponseDTO dto = service.consultar("49010-000");

        assertEquals("49010000", dto.cep());
        assertEquals("Rua A", dto.logradouro());
        assertEquals("Centro", dto.bairro());
        assertEquals("Aracaju", dto.cidade());
        assertEquals("SE", dto.uf());
    }

    @Test
    void consultar_cepComTamanhoErrado_lancaCepInvalido() {
        assertThrows(CepInvalidoException.class, () -> service.consultar("123"));
        verifyNoInteractions(cepGateway);
    }

    @Test
    void consultar_cepInexistente_lancaNaoEncontrado() {
        when(cepGateway.consultar("00000000")).thenReturn(Optional.empty());

        assertThrows(CepNaoEncontradoException.class, () -> service.consultar("00000000"));
    }

    @Test
    void consultar_servicoFora_propagaIndisponivel() {
        when(cepGateway.consultar("49010000")).thenThrow(new CepIndisponivelException());

        assertThrows(CepIndisponivelException.class, () -> service.consultar("49010000"));
    }

    @Test
    void consultar_cepSemTraco_devolveEndereco() {
        when(cepGateway.consultar("49010000")).thenReturn(
                Optional.of(new EnderecoCep("49010000", "Rua A", "Centro", "Aracaju", "SE")));

        CepResponseDTO dto = service.consultar("49010000");

        assertEquals("49010000", dto.cep());
        assertEquals("Aracaju", dto.cidade());
    }

    @Test
    void consultar_cepComZeroInicial_preservaDigitos() {
        when(cepGateway.consultar("01234567")).thenReturn(
                Optional.of(new EnderecoCep("01234567", "Rua A", "Centro", "São Paulo", "SP")));

        CepResponseDTO dto = service.consultar("01234-567");

        assertEquals("01234567", dto.cep());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "abc49010000",
            "49010000xyz",
            "49010@000",
            "4901-0000",
            "49010--000",
            "1234567",
            "123456789",
            "49010 000",
            " 49010000",
            "49010000 ",
            " "
    })
    void consultar_formatoInvalido_naoConsultaGateway(String cep) {
        assertThrows(CepInvalidoException.class, () -> service.consultar(cep));
        verifyNoInteractions(cepGateway);
    }
}
