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

        assertEquals("Rua A", dto.logradouro());
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
}
