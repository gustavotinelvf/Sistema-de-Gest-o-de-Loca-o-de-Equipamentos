package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.StatusUsuario;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

class AnonimizacaoUsuariosServiceTest {

    private static final Instant AGORA =
            Instant.parse("2026-11-27T12:00:00Z");

    private static final Instant LIMITE =
            Instant.parse("2025-11-27T12:00:00Z");

    private final UsuarioRepository repository =
            mock(UsuarioRepository.class);

    private final AnonimizacaoUsuariosService service =
            new AnonimizacaoUsuariosService(repository, 365);

    private Usuario inativo(long id, Instant dataInativacao) {
        Usuario u = Usuario.criarAtivo(
                1L, "Pessoa " + id, "p" + id + "@x.com",
                "hash", Perfil.ATENDENTE, null);

        ReflectionTestUtils.setField(u, "id", id);
        u.inativar(dataInativacao);

        return u;
    }

    @Test
    void anonimizarInativos_usaLimiteDoPrazoEAnonimizaTodos() {
        Usuario a = inativo(1L, LIMITE.minusSeconds(1));
        Usuario b = inativo(2L, LIMITE.minusSeconds(86400));

        when(repository.findInativosParaAnonimizacao(LIMITE))
                .thenReturn(List.of(a, b));

        int total = service.anonimizarInativos(AGORA);

        assertEquals(2, total);
        assertTrue(a.isAnonimizado());
        assertTrue(b.isAnonimizado());

        assertEquals(AGORA, a.getAnonimizadoEm());
        assertEquals(AGORA, b.getAnonimizadoEm());

        assertNull(a.getSenhaHash());
        assertNull(b.getSenhaHash());

        assertEquals(StatusUsuario.INATIVO, a.getStatus());
        assertEquals(StatusUsuario.INATIVO, b.getStatus());

        verify(repository).findInativosParaAnonimizacao(LIMITE);
        verify(repository).saveAll(List.of(a, b));
    }

    @Test
    void anonimizarInativos_semCandidatos_devolveZero() {
        when(repository.findInativosParaAnonimizacao(LIMITE))
                .thenReturn(List.of());

        assertEquals(0, service.anonimizarInativos(AGORA));

        verify(repository).findInativosParaAnonimizacao(LIMITE);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void anonimizarInativos_prazoMenorGeraLimiteMaisRecente() {
        AnonimizacaoUsuariosService curto =
                new AnonimizacaoUsuariosService(repository, 30);

        Instant limiteCurto =
                Instant.parse("2026-10-28T12:00:00Z");

        Usuario u = inativo(3L, limiteCurto);

        when(repository.findInativosParaAnonimizacao(limiteCurto))
                .thenReturn(List.of(u));

        assertEquals(1, curto.anonimizarInativos(AGORA));
        assertTrue(u.isAnonimizado());

        verify(repository).findInativosParaAnonimizacao(limiteCurto);
        verify(repository).saveAll(List.of(u));
    }

    @Test
    void construtor_prazoInvalido_rejeita() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnonimizacaoUsuariosService(repository, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new AnonimizacaoUsuariosService(repository, -5));

        verifyNoInteractions(repository);
    }

    @Test
    void agendador_executar_chamaOServico() {
        AnonimizacaoUsuariosService servico =
                mock(AnonimizacaoUsuariosService.class);

        when(servico.anonimizarInativos(any(Instant.class)))
                .thenReturn(4);

        new AnonimizacaoUsuariosAgendador(servico).executar();

        verify(servico).anonimizarInativos(any(Instant.class));
    }

    @Test
    void anonimizarInativos_dataReferenciaNula_rejeita() {
        assertThrows(NullPointerException.class,
                () -> service.anonimizarInativos(null));

        verifyNoInteractions(repository);
    }

    @Test
    void construtor_repositoryNulo_rejeita() {
        assertThrows(NullPointerException.class,
                () -> new AnonimizacaoUsuariosService(null, 365));
    }

    @Test
    void anonimizarInativos_candidatoNoLimiteExato_anonimiza() {
        Usuario u = inativo(4L, LIMITE);

        when(repository.findInativosParaAnonimizacao(LIMITE))
                .thenReturn(List.of(u));

        assertEquals(1, service.anonimizarInativos(AGORA));
        assertTrue(u.isAnonimizado());
        assertEquals(LIMITE, u.getDataInativacao());
        assertEquals(AGORA, u.getAnonimizadoEm());

        verify(repository).saveAll(List.of(u));
    }

    @Test
    void anonimizarInativos_prazoDeUmDia_calculaLimiteCorreto() {
        AnonimizacaoUsuariosService curto =
                new AnonimizacaoUsuariosService(repository, 1);

        Instant limite = AGORA.minus(1, ChronoUnit.DAYS);

        when(repository.findInativosParaAnonimizacao(limite))
                .thenReturn(List.of());

        assertEquals(0, curto.anonimizarInativos(AGORA));

        verify(repository).findInativosParaAnonimizacao(limite);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void anonimizarInativos_execucaoPosteriorSemCandidatos_naoRepeteAnonimizacao() {
        Usuario u = inativo(5L, LIMITE);

        Instant proximaExecucao = AGORA.plus(1, ChronoUnit.DAYS);
        Instant proximoLimite = LIMITE.plus(1, ChronoUnit.DAYS);

        when(repository.findInativosParaAnonimizacao(LIMITE))
                .thenReturn(List.of(u));

        when(repository.findInativosParaAnonimizacao(proximoLimite))
                .thenReturn(List.of());

        assertEquals(1, service.anonimizarInativos(AGORA));
        assertEquals(0, service.anonimizarInativos(proximaExecucao));

        assertEquals(AGORA, u.getAnonimizadoEm());

        verify(repository).saveAll(List.of(u));
        verify(repository, never()).saveAll(List.of());
    }

    @Test
    void anonimizarInativos_candidatoComEstadoInvalido_propagaErro() {
        Usuario ativo = Usuario.criarAtivo(
                1L, "Pessoa", "pessoa@x.com",
                "hash", Perfil.ATENDENTE, null);

        ReflectionTestUtils.setField(ativo, "id", 6L);

        when(repository.findInativosParaAnonimizacao(LIMITE))
                .thenReturn(List.of(ativo));

        assertThrows(IllegalStateException.class,
                () -> service.anonimizarInativos(AGORA));

        assertFalse(ativo.isAnonimizado());
        assertTrue(ativo.isAtivo());

        verify(repository, never()).saveAll(any());
    }

    @Test
    void agendador_possuiCronEFusoConfiguraveis()
            throws NoSuchMethodException {

        Scheduled configuracao =
                AnonimizacaoUsuariosAgendador.class
                        .getMethod("executar")
                        .getAnnotation(Scheduled.class);

        assertNotNull(configuracao);

        assertEquals(
                "${ziploc.lgpd.cron:0 0 3 * * *}",
                configuracao.cron());

        assertEquals(
                "${ziploc.lgpd.zone:America/Sao_Paulo}",
                configuracao.zone());
    }
}