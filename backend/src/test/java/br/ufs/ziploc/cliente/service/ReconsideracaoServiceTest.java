package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.RegistrarReconsideracaoDTO;
import br.ufs.ziploc.cliente.dto.ReconsideracaoResponseDTO;
import br.ufs.ziploc.cliente.repository.ReconsideracaoRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconsideracaoServiceTest {

    private static final Long CLIENTE_ID = 1L;
    private static final Long LOCADORA_ID = 2L;
    private static final Long GERENTE_ID = 3L;

    @Mock
    private ReconsideracaoRepository reconsideracoes;

    @InjectMocks
    private ReconsideracaoService service;

    private RegistrarReconsideracaoDTO request(
            DecisaoReconsideracao decisao) {

        return new RegistrarReconsideracaoDTO(
                decisao,
                "Decisão registrada após reunião com o cliente."
        );
    }

    private void autorizarGerente() {

        when(reconsideracoes.gerenteAutorizado(
                GERENTE_ID,
                LOCADORA_ID
        )).thenReturn(true);
    }

    private void configurarCliente(
            StatusAdimplencia status,
            boolean ativo) {

        when(reconsideracoes.buscarClienteParaAtualizacao(
                CLIENTE_ID,
                LOCADORA_ID
        )).thenReturn(Optional.of(
                new ReconsideracaoRepository.ClienteCiclo(
                        CLIENTE_ID,
                        1,
                        status,
                        ativo
                )
        ));
    }

    @Test
    void deveAprovarReconsideracaoEIniciarNovoCiclo() {

        autorizarGerente();

        configurarCliente(
                StatusAdimplencia.INADIMPLENTE,
                true
        );

        when(reconsideracoes.registrarDecisao(
                eq(LOCADORA_ID),
                eq(CLIENTE_ID),
                eq(GERENTE_ID),
                eq(1),
                eq(DecisaoReconsideracao.APROVADA),
                anyString(),
                any(Instant.class)
        )).thenReturn(10L);

        when(reconsideracoes.iniciarNovoCiclo(
                CLIENTE_ID,
                LOCADORA_ID,
                1
        )).thenReturn(1);

        ReconsideracaoResponseDTO resultado =
                service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                );

        assertEquals(10L, resultado.id());

        assertEquals(
                StatusAdimplencia.ADIMPLENTE,
                resultado.statusAdimplencia()
        );

        assertEquals(1, resultado.cicloAnterior());
        assertEquals(2, resultado.cicloAtual());

        assertNotNull(resultado.dataDecisao());

        verify(reconsideracoes).iniciarNovoCiclo(
                CLIENTE_ID,
                LOCADORA_ID,
                1
        );
    }

    @Test
    void devePreservarInadimplenciaQuandoReconsideracaoRejeitada() {

        autorizarGerente();

        configurarCliente(
                StatusAdimplencia.INADIMPLENTE,
                true
        );

        when(reconsideracoes.registrarDecisao(
                eq(LOCADORA_ID),
                eq(CLIENTE_ID),
                eq(GERENTE_ID),
                eq(1),
                eq(DecisaoReconsideracao.REJEITADA),
                anyString(),
                any(Instant.class)
        )).thenReturn(11L);

        ReconsideracaoResponseDTO resultado =
                service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.REJEITADA)
                );

        assertEquals(11L, resultado.id());

        assertEquals(
                StatusAdimplencia.INADIMPLENTE,
                resultado.statusAdimplencia()
        );

        assertEquals(1, resultado.cicloAnterior());
        assertEquals(1, resultado.cicloAtual());

        verify(reconsideracoes, never())
                .iniciarNovoCiclo(
                        anyLong(),
                        anyLong(),
                        anyInt()
                );
    }

    @Test
    void deveImpedirGerenteNaoAutorizado() {

        when(reconsideracoes.gerenteAutorizado(
                GERENTE_ID,
                LOCADORA_ID
        )).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );

        verifyNoMoreInteractions(reconsideracoes);
    }

    @Test
    void deveRejeitarClienteInexistenteOuDeOutraLocadora() {

        autorizarGerente();

        when(reconsideracoes.buscarClienteParaAtualizacao(
                CLIENTE_ID,
                LOCADORA_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );

        verify(reconsideracoes, never())
                .iniciarNovoCiclo(
                        anyLong(),
                        anyLong(),
                        anyInt()
                );
    }

    @Test
    void deveImpedirReconsideracaoDeClienteAdimplente() {

        autorizarGerente();

        configurarCliente(
                StatusAdimplencia.ADIMPLENTE,
                true
        );

        assertThrows(
                ConflitoException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );

        verify(reconsideracoes, never())
                .iniciarNovoCiclo(
                        anyLong(),
                        anyLong(),
                        anyInt()
                );
    }

    @Test
    void deveImpedirReconsideracaoDeClienteInativo() {

        autorizarGerente();

        configurarCliente(
                StatusAdimplencia.INADIMPLENTE,
                false
        );

        assertThrows(
                ConflitoException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );
    }

    @Test
    void deveExigirJustificativa() {

        RegistrarReconsideracaoDTO dados =
                new RegistrarReconsideracaoDTO(
                        DecisaoReconsideracao.APROVADA,
                        "   "
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        dados
                )
        );

        verifyNoInteractions(reconsideracoes);
    }

    @Test
    void deveRejeitarJustificativaAcimaDoLimite() {

        RegistrarReconsideracaoDTO dados =
                new RegistrarReconsideracaoDTO(
                        DecisaoReconsideracao.APROVADA,
                        "a".repeat(2001)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        dados
                )
        );

        verifyNoInteractions(reconsideracoes);
    }

    @Test
    void deveExigirDecisao() {

        RegistrarReconsideracaoDTO dados =
                new RegistrarReconsideracaoDTO(
                        null,
                        "Justificativa válida."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        dados
                )
        );

        verifyNoInteractions(reconsideracoes);
    }

    @Test
    void deveRejeitarIdentificadorInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        -1L,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );

        verifyNoInteractions(reconsideracoes);
    }

    @Test
    void deveLancarConflitoQuandoNovoCicloNaoForAtualizado() {

        autorizarGerente();

        configurarCliente(
                StatusAdimplencia.INADIMPLENTE,
                true
        );

        when(reconsideracoes.registrarDecisao(
                eq(LOCADORA_ID),
                eq(CLIENTE_ID),
                eq(GERENTE_ID),
                eq(1),
                eq(DecisaoReconsideracao.APROVADA),
                anyString(),
                any(Instant.class)
        )).thenReturn(12L);

        when(reconsideracoes.iniciarNovoCiclo(
                CLIENTE_ID,
                LOCADORA_ID,
                1
        )).thenReturn(0);

        assertThrows(
                ConflitoException.class,
                () -> service.registrar(
                        CLIENTE_ID,
                        LOCADORA_ID,
                        GERENTE_ID,
                        request(DecisaoReconsideracao.APROVADA)
                )
        );
    }
}