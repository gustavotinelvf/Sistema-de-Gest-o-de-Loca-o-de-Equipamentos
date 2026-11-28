package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnonimizacaoUsuariosServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final AnonimizacaoUsuariosService service = new AnonimizacaoUsuariosService(repository, 365);

    private Usuario inativo(long id) {
        Usuario u = Usuario.criarAtivo(1L, "Pessoa " + id, "p" + id + "@x.com", "hash", Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "id", id);
        u.inativar();
        return u;
    }

    @Test
    void anonimizarInativos_usaLimiteDoPrazoEAnonimizaTodos() {
        Usuario a = inativo(1L);
        Usuario b = inativo(2L);
        Instant limiteEsperado = Instant.parse("2025-11-27T12:00:00Z");
        when(repository.findInativosParaAnonimizacao(limiteEsperado)).thenReturn(List.of(a, b));

        int total = service.anonimizarInativos(AGORA);

        assertEquals(2, total);
        assertTrue(a.isAnonimizado());
        assertTrue(b.isAnonimizado());
        verify(repository).saveAll(List.of(a, b));
    }

    @Test
    void anonimizarInativos_semCandidatos_devolveZero() {
        when(repository.findInativosParaAnonimizacao(Instant.parse("2025-11-27T12:00:00Z")))
                .thenReturn(List.of());

        assertEquals(0, service.anonimizarInativos(AGORA));
        verify(repository).saveAll(List.of());
    }

    @Test
    void anonimizarInativos_prazoMenorGeraLimiteMaisRecente() {
        AnonimizacaoUsuariosService curto = new AnonimizacaoUsuariosService(repository, 30);
        when(repository.findInativosParaAnonimizacao(Instant.parse("2026-10-28T12:00:00Z")))
                .thenReturn(List.of(inativo(3L)));

        assertEquals(1, curto.anonimizarInativos(AGORA));
    }

    @Test
    void construtor_prazoInvalido_rejeita() {
        assertThrows(IllegalArgumentException.class, () -> new AnonimizacaoUsuariosService(repository, 0));
        assertThrows(IllegalArgumentException.class, () -> new AnonimizacaoUsuariosService(repository, -5));
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void agendador_executar_chamaOServico() {
        AnonimizacaoUsuariosService servico = mock(AnonimizacaoUsuariosService.class);
        when(servico.anonimizarInativos(org.mockito.ArgumentMatchers.any())).thenReturn(4);

        new AnonimizacaoUsuariosAgendador(servico).executar();

        verify(servico).anonimizarInativos(org.mockito.ArgumentMatchers.any(Instant.class));
    }
}
