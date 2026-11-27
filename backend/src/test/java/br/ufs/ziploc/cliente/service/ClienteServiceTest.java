package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.Contato;
import br.ufs.ziploc.cliente.domain.Endereco;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.ClienteRequestDTO;
import br.ufs.ziploc.cliente.dto.ClienteResponseDTO;
import br.ufs.ziploc.cliente.repository.ClienteRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LocadoraApi locadoraApi;

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
        return new Cliente(1L, "Construtora Horizonte Ltda", "11222333000181", contato, criarEnderecoValido());
    }

    private ClienteRequestDTO criarRequestValido(String cnpj) {
        return new ClienteRequestDTO(
                "Construtora Horizonte Ltda",
                cnpj,
                "Maria Silva",
                "12345678901",
                "maria@construtora.com",
                "79999990000",
                "49000-000",
                "Av. Beira Mar",
                "100",
                "Sala 2",
                "Jabotiana",
                "Aracaju",
                "se"
        );
    }

    private void simularLocadora(Long locadoraId) {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);
        when(parametros.id()).thenReturn(locadoraId);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
    }

    @Test
    void deveCadastrarClienteNormalizandoCnpjCepEUf_RF001_RD007() {
        ClienteRequestDTO request = criarRequestValido("11.222.333/0001-81");
        simularLocadora(1L);
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.cadastrar(request);

        assertEquals(1L, resposta.locadoraId());
        assertEquals("Construtora Horizonte Ltda", resposta.razaoSocial());
        assertEquals("11222333000181", resposta.cnpj());
        assertEquals("49000000", resposta.cep());
        assertEquals("SE", resposta.uf());
        assertEquals(StatusAdimplencia.ADIMPLENTE, resposta.statusAdimplencia());
        assertTrue(resposta.ativo());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void deveRejeitarCadastroComCnpjInvalido_RD007() {
        ClienteRequestDTO request = criarRequestValido("11.222.333/0001-99");

        assertThrows(IllegalArgumentException.class, () -> clienteService.cadastrar(request));
        verifyNoInteractions(clienteRepository, locadoraApi);
    }

    @Test
    void deveLancarConflitoQuandoCnpjJaExistir_RD008() {
        ClienteRequestDTO request = criarRequestValido("11222333000181");
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(locadoraApi);
    }

    @Test
    void deveListarClientesPaginadosComFiltrosDaLocadora_RF004() {
        simularLocadora(1L);
        Cliente cliente = criarClienteValido();
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(cliente), pageable, 1));

        Page<ClienteResponseDTO> pagina = clienteService.listar(
                "Horizonte", "11.222.333/0001-81", true, StatusAdimplencia.ADIMPLENTE, pageable
        );

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Construtora Horizonte Ltda", pagina.getContent().get(0).razaoSocial());
        assertEquals("11222333000181", pagina.getContent().get(0).cnpj());
    }

    @Test
    void deveBuscarClientePorIdDaLocadora_RF004() {
        simularLocadora(1L);
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findByIdAndLocadoraId(10L, 1L)).thenReturn(Optional.of(cliente));

        ClienteResponseDTO resposta = clienteService.buscarPorId(10L);

        assertEquals("Construtora Horizonte Ltda", resposta.razaoSocial());
        assertEquals("11222333000181", resposta.cnpj());
    }

    @Test
    void deveLancarExcecaoAoBuscarPorIdInexistente_RF004() {
        simularLocadora(1L);
        when(clienteRepository.findByIdAndLocadoraId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> clienteService.buscarPorId(99L));
    }

    @Test
    void deveBuscarClientePorCnpjLimpandoMascara_RF004() {
        simularLocadora(1L);
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findByCnpjAndLocadoraId("11222333000181", 1L)).thenReturn(Optional.of(cliente));

        ClienteResponseDTO resposta = clienteService.buscarPorCnpj("11.222.333/0001-81");

        assertEquals("Construtora Horizonte Ltda", resposta.razaoSocial());
        assertEquals("11222333000181", resposta.cnpj());
    }

    @Test
    void deveLancarExcecaoAoBuscarPorCnpjInexistente_RF004() {
        simularLocadora(1L);
        when(clienteRepository.findByCnpjAndLocadoraId("04252011000110", 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> clienteService.buscarPorCnpj("04.252.011/0001-10"));
    }

    @Test
    void deveConsultarResumoDoClienteQuandoExistir() {
        Cliente cliente = criarClienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResumoDTO resumo = clienteService.consultarResumo(1L);

        assertEquals(1L, resumo.locadoraId());
        assertEquals("Construtora Horizonte Ltda", resumo.razaoSocial());
        assertEquals("11222333000181", resumo.cnpj());
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
                1L, "Construtora Horizonte Ltda", "11222333000181", null, criarEnderecoValido()
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

        assertThrows(RecursoNaoEncontradoException.class, () -> clienteService.consultarResumo(99L));
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
                RecursoNaoEncontradoException.class,
                () -> clienteService.alterarStatusAdimplencia(99L, StatusAdimplencia.INADIMPLENTE)
        );
        verify(clienteRepository, never()).save(any());
    }
}