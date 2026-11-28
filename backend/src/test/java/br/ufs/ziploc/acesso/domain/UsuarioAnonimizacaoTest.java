package br.ufs.ziploc.acesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UsuarioAnonimizacaoTest {

    private Usuario inativo(long id, Perfil perfil, String cpf) {
        Usuario u = Usuario.criarAtivo(1L, "Ana Souza", "ana@x.com", "hash", perfil, cpf);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "cargo", "Atendente sênior");
        u.inativar();
        return u;
    }

    @Test
    void anonimizar_inativo_removeDadosPessoaisECredenciais() {
        Usuario u = inativo(7L, Perfil.GERENTE, "12345678901");
        Instant agora = Instant.parse("2026-11-27T12:00:00Z");

        u.anonimizar(agora);

        assertTrue(u.isAnonimizado());
        assertEquals(agora, u.getAnonimizadoEm());
        assertEquals("ANONIMIZADO", u.getNome());
        assertEquals("anonimizado-7@anonimizado.invalid", u.getEmail());
        assertNull(u.getCpf());
        assertNull(u.getSenhaHash());
        assertNull(u.getCargo());
    }

    @Test
    void anonimizar_mantemInativoEPerfil_paraRespeitarOsChecksDoBanco() {
        Usuario u = inativo(8L, Perfil.GERENTE, "12345678901");

        u.anonimizar(Instant.now());

        assertEquals(StatusUsuario.INATIVO, u.getStatus());
        assertEquals(Perfil.GERENTE, u.getPerfil());
        assertFalse(u.isAtivo());
    }

    @Test
    void anonimizar_emailDependeDoId_logoNuncaColide() {
        Usuario a = inativo(1L, Perfil.ATENDENTE, null);
        Usuario b = inativo(2L, Perfil.ATENDENTE, null);

        a.anonimizar(Instant.now());
        b.anonimizar(Instant.now());

        assertFalse(a.getEmail().equalsIgnoreCase(b.getEmail()));
    }

    @Test
    void anonimizar_usuarioAtivo_rejeita() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "id", 3L);

        IllegalStateException erro =
                assertThrows(IllegalStateException.class, () -> u.anonimizar(Instant.now()));

        assertEquals("Só um usuário INATIVO pode ser anonimizado", erro.getMessage());
        assertEquals("Ana", u.getNome());
    }

    @Test
    void anonimizar_usuarioJaAnonimizado_rejeita() {
        Usuario u = inativo(4L, Perfil.ATENDENTE, null);
        u.anonimizar(Instant.now());

        IllegalStateException erro =
                assertThrows(IllegalStateException.class, () -> u.anonimizar(Instant.now()));

        assertEquals("O usuário já foi anonimizado", erro.getMessage());
    }

    @Test
    void anonimizar_semId_rejeita() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.ATENDENTE, null);
        u.inativar();

        assertThrows(IllegalStateException.class, () -> u.anonimizar(Instant.now()));
    }

    @Test
    void anonimizar_semData_rejeita() {
        Usuario u = inativo(5L, Perfil.ATENDENTE, null);

        assertThrows(NullPointerException.class, () -> u.anonimizar(null));
    }

    @Test
    void anonimizado_naoPodeSerReativado() {
        Usuario u = inativo(6L, Perfil.ATENDENTE, null);
        u.anonimizar(Instant.now());

        assertThrows(IllegalStateException.class, u::ativar);
    }
}
