package br.ufs.ziploc.acesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void criarAtivo_gerenteSemCpf_rejeita_RD016() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.GERENTE, null));
    }

    @Test
    void criarAtivo_gerenteComCpf_ficaAtivo() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");
        assertEquals(StatusUsuario.ATIVO, u.getStatus());
        assertEquals(1L, u.getLocadoraId());
        assertNotNull(u.getDataAtivacao());
        assertTrue(u.isAtivo());
    }

    @Test
    void criarAtivo_atendenteSemCpf_aceita() {
        Usuario u = Usuario.criarAtivo(1L, "Bia", "bia@x.com", "hash", Perfil.ATENDENTE, "");
        assertNull(u.getCpf());
    }

    @Test
    void criarAtivo_semLocadora_rejeita() {
        assertThrows(NullPointerException.class,
                () -> Usuario.criarAtivo(null, "Bia", "bia@x.com", "hash", Perfil.ATENDENTE, null));
    }

    @Test
    void criarPendente_naoTemSenhaENaoEstaAtivo() {
        Usuario u = Usuario.criarPendente(1L, "Caio", "caio@x.com", Perfil.ESTOQUISTA, "Estoque");
        assertEquals(StatusUsuario.PENDENTE, u.getStatus());
        assertNull(u.getSenhaHash());
        assertNull(u.getDataAtivacao());
        assertFalse(u.isAtivo());
    }

    @Test
    void concluirCadastro_ativaComSenha() {
        Usuario u = Usuario.criarPendente(1L, "Caio", "caio@x.com", Perfil.ESTOQUISTA, null);
        Instant agora = Instant.now();
        u.concluirCadastro("hash-bcrypt", null, agora);
        assertTrue(u.isAtivo());
        assertEquals("hash-bcrypt", u.getSenhaHash());
        assertEquals(agora, u.getDataAtivacao());
    }

    @Test
    void concluirCadastro_gerenteSemCpf_rejeita_RD016() {
        Usuario u = Usuario.criarPendente(1L, "Ana", "ana@x.com", Perfil.GERENTE, null);
        assertThrows(IllegalArgumentException.class,
                () -> u.concluirCadastro("hash", null, Instant.now()));
        assertEquals(StatusUsuario.PENDENTE, u.getStatus());
    }

    @Test
    void concluirCadastro_gerenteInformandoCpf_ativa() {
        Usuario u = Usuario.criarPendente(1L, "Ana", "ana@x.com", Perfil.GERENTE, null);
        u.concluirCadastro("hash", "12345678901", Instant.now());
        assertEquals("12345678901", u.getCpf());
        assertTrue(u.isAtivo());
    }

    @Test
    void concluirCadastro_usuarioJaAtivo_rejeita() {
        Usuario u = Usuario.criarAtivo(1L, "Bia", "bia@x.com", "hash", Perfil.ATENDENTE, null);
        assertThrows(IllegalStateException.class,
                () -> u.concluirCadastro("outra", null, Instant.now()));
    }

    @Test
    void inativar_e_ativar_alteramStatus() {
        Usuario u = Usuario.criarAtivo(1L, "Bia", "bia@x.com", "hash", Perfil.ATENDENTE, null);
        u.inativar();
        assertFalse(u.isAtivo());
        u.ativar();
        assertTrue(u.isAtivo());
    }

    @Test
    void ativar_usuarioSemSenha_rejeita() {
        Usuario u = Usuario.criarPendente(1L, "Caio", "caio@x.com", Perfil.ESTOQUISTA, null);
        u.inativar();
        assertThrows(IllegalStateException.class, u::ativar);
    }

    @Test
    void perfil_marcaGrupoAdministrativo() {
        assertTrue(Perfil.GERENTE.isGrupoAdministrativo());
        assertTrue(Perfil.FINANCEIRO.isGrupoAdministrativo());
        assertFalse(Perfil.ATENDENTE.isGrupoAdministrativo());
    }
}
