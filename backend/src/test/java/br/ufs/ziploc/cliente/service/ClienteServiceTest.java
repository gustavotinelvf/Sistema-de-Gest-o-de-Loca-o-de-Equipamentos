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
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

    private void configurarLocadora() {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);
        when(parametros.id()).thenReturn(1L);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
    }

    @Test
    void deveCadastrarClienteNormalizandoCnpjCepEUf_RF001_RD007() {
        ClienteRequestDTO request = criarRequestValido("11.222.333/0001-81");

        configurarLocadora();
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.cadastrar(request);

        assertEquals(1L, resposta.locadoraId());
        assertEquals("Construtora Horizonte Ltda", resposta.razaoSocial());
        assertEquals("11222333000181", resposta.cnpj());
        assertEquals("Maria Silva", resposta.representanteNome());
        assertEquals("12345678901", resposta.representanteCpf());
        assertEquals("maria@construtora.com", resposta.email());
        assertEquals("79999990000", resposta.telefone());
        assertEquals("49000000", resposta.cep());
        assertEquals("Av. Beira Mar", resposta.logradouro());
        assertEquals("100", resposta.numero());
        assertEquals("Sala 2", resposta.complemento());
        assertEquals("Jabotiana", resposta.bairro());
        assertEquals("Aracaju", resposta.cidade());
        assertEquals("SE", resposta.uf());
        assertEquals(StatusAdimplencia.ADIMPLENTE, resposta.statusAdimplencia());
        assertTrue(resposta.ativo());
        assertNull(resposta.dataAnonimizacao());

        verify(clienteRepository).saveAndFlush(any(Cliente.class));
    }

    @Test
    void deveRejeitarCadastroComCnpjInvalido_RD007() {
        ClienteRequestDTO request = criarRequestValido("11.222.333/0001-99");

        assertThrows(IllegalArgumentException.class, () -> clienteService.cadastrar(request));
        verifyNoInteractions(clienteRepository, locadoraApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc11222333000181",
            "11222333000181xyz",
            "11@222@333@0001@81",
            "11.222333/0001-81",
            "11222333000181-",
            " 11222333000181",
            "11222333000181 "
    })
    void deveRejeitarCnpjComFormatoInvalido(String cnpj) {
        ClienteRequestDTO request = criarRequestValido(cnpj);

        assertThrows(IllegalArgumentException.class, () -> clienteService.cadastrar(request));
        verifyNoInteractions(clienteRepository, locadoraApi);
    }

    @Test
    void deveLancarConflitoQuandoCnpjJaExistir_RD008() {
        ClienteRequestDTO request = criarRequestValido("11222333000181");
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(true);

        ConflitoException ex = assertThrows(
                ConflitoException.class,
                () -> clienteService.cadastrar(request)
        );

        assertEquals("Já existe um cliente cadastrado com o CNPJ informado.", ex.getMessage());
        verify(clienteRepository, never()).saveAndFlush(any());
        verifyNoInteractions(locadoraApi);
    }

    @Test
    void deveLancarConflitoQuandoCnpjComMascaraJaExistir() {
        ClienteRequestDTO request = criarRequestValido("11.222.333/0001-81");
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));

        verify(clienteRepository).existsByCnpj("11222333000181");
        verify(clienteRepository, never()).saveAndFlush(any());
        verifyNoInteractions(locadoraApi);
    }

    @Test
    void deveTratarViolacaoDeUnicidadeDoCnpj() {
        ClienteRequestDTO request = criarRequestValido("11222333000181");

        configurarLocadora();
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(false);

        ConstraintViolationException violacao = mock(ConstraintViolationException.class);
        when(violacao.getConstraintName()).thenReturn("cliente_cnpj_key");

        when(clienteRepository.saveAndFlush(any(Cliente.class)))
                .thenThrow(new DataIntegrityViolationException("CNPJ duplicado", violacao));

        ConflitoException ex = assertThrows(
                ConflitoException.class,
                () -> clienteService.cadastrar(request)
        );

        assertEquals("Já existe um cliente cadastrado com o CNPJ informado.", ex.getMessage());
        verify(clienteRepository).saveAndFlush(any(Cliente.class));
    }

    @Test
    void devePreservarOutrasViolacoesDeIntegridade() {
        ClienteRequestDTO request = criarRequestValido("11222333000181");

        configurarLocadora();
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(false);

        ConstraintViolationException violacao = mock(ConstraintViolationException.class);
        when(violacao.getConstraintName()).thenReturn("cliente_locadora_id_fkey");

        DataIntegrityViolationException erro =
                new DataIntegrityViolationException("Locadora inexistente", violacao);

        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenThrow(erro);

        DataIntegrityViolationException ex = assertThrows(
                DataIntegrityViolationException.class,
                () -> clienteService.cadastrar(request)
        );

        assertSame(erro, ex);
    }

    @Test
    void deveCadastrarClienteSemDadosOpcionais() {
        ClienteRequestDTO request = new ClienteRequestDTO(
                "Construtora Horizonte Ltda",
                "11222333000181",
                null,
                null,
                null,
                null,
                "49000000",
                "Av. Beira Mar",
                "100",
                null,
                "Jabotiana",
                "Aracaju",
                "SE"
        );

        configurarLocadora();
        when(clienteRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.cadastrar(request);

        assertNull(resposta.representanteNome());
        assertNull(resposta.representanteCpf());
        assertNull(resposta.email());
        assertNull(resposta.telefone());
        assertNull(resposta.complemento());
        assertTrue(resposta.ativo());
        assertEquals(StatusAdimplencia.ADIMPLENTE, resposta.statusAdimplencia());

        verify(clienteRepository).saveAndFlush(
                argThat(cliente -> cliente.getContato() == null)
        );
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
        assertEquals("Av. Beira Mar", resumo.logradouro());
        assertEquals("100", resumo.numero());
        assertEquals("Sala 2", resumo.complemento());
        assertEquals("Jabotiana", resumo.bairro());
        assertEquals("Aracaju", resumo.cidade());
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
