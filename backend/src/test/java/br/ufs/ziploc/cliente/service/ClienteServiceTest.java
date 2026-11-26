package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.Contato;
import br.ufs.ziploc.cliente.domain.Endereco;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Endereco criarEnderecoValido() {
        return new Endereco(
                "49000000", "Av. Beira Mar", "100", "Sala 2",
                "Jabotiana", "Aracaju", "SE"
        );
    }

    private Cliente criarClienteValido() {
        Contato contato = new Contato("Maria Silva", "12345678901", "maria@construtora.com", "79999990000");
        return new Cliente(1L, "Construtora Horizonte Ltda", "12345678000199", contato, criarEnderecoValido());
    }

    @Test
    void deveConsultarResumoDoClienteQuandoExistir() {
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResumoDTO resumo = clienteService.consultarResumo(1L);

        assertEquals(1L, resumo.locadoraId());
        assertEquals("Construtora Horizonte Ltda", resumo.razaoSocial());
        assertEquals("12345678000199", resumo.cnpj());
        assertEquals("Maria Silva", resumo.representanteNome());
        assertEquals("12345678901", resumo.representanteCpf());
        assertEquals("maria@construtora.com", resumo.email());
        assertEquals("79999990000", resumo.telefone());
        assertEquals(StatusAdimplencia.ADIMPLENTE, resumo.statusAdimplencia());
        assertTrue(resumo.ativo());
        assertEquals("49000000", resumo.cep());
        assertEquals("SE", resumo.uf());
    }

    @Test
    void deveConsultarResumoSemErroQuandoContatoForNulo() {
        Cliente clienteSemContato = new Cliente(
                1L, "Construtora Horizonte Ltda", "12345678000199", null, criarEnderecoValido()
        );
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteSemContato));

        ClienteResumoDTO resumo = clienteService.consultarResumo(2L);

        assertEquals("Construtora Horizonte Ltda", resumo.razaoSocial());
        assertNull(resumo.representanteNome());
        assertNull(resumo.representanteCpf());
        assertNull(resumo.email());
        assertNull(resumo.telefone());
    }

    @Test
    void deveLancarExcecaoAoConsultarResumoDeClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.consultarResumo(99L));
    }

    @Test
    void deveRetornarTrueQuandoClienteEstiverAdimplente_RD011() {
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertTrue(clienteService.verificarAdimplencia(1L));
    }

    @Test
    void deveRetornarFalseQuandoClienteEstiverInadimplente_RD011() {
        Cliente cliente = criarClienteValido();
        cliente.alterarStatusAdimplencia(StatusAdimplencia.INADIMPLENTE);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertFalse(clienteService.verificarAdimplencia(1L));
    }

    @Test
    void deveAlterarStatusDeAdimplenciaESalvarCliente_RF018_RD010() {
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.alterarStatusAdimplencia(1L, StatusAdimplencia.INADIMPLENTE);

        assertEquals(StatusAdimplencia.INADIMPLENTE, cliente.getStatusAdimplencia());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void deveLancarExcecaoAoAlterarStatusDeClienteInexistente_RF018() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> clienteService.alterarStatusAdimplencia(99L, StatusAdimplencia.INADIMPLENTE)
        );
        verify(clienteRepository, never()).save(any());
    }
}