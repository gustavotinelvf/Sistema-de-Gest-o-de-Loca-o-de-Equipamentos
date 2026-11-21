package br.ufs.ziploc.acesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void criarAtivo_gerenteSemCpf_rejeita_RD016() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarAtivo("Ana", "ana@x.com", "hash", Perfil.GERENTE, null));
    }

    @Test
    void criarAtivo_gerenteComCpf_ficaAtivo() {
        Usuario u = Usuario.criarAtivo("Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");
        assertEquals(StatusUsuario.ATIVO, u.getStatus());
        assertNotNull(u.getDataAtivacao());
        assertTrue(u.isAtivo());
    }

    @Test
    void criarAtivo_atendenteSemCpf_aceita() {
        Usuario u = Usuario.criarAtivo("Bia", "bia@x.com", "hash", Perfil.ATENDENTE, "");
        assertNull(u.getCpf());
    }

    @Test
    void inativar_e_ativar_alteramStatus() {
        Usuario u = Usuario.criarAtivo("Bia", "bia@x.com", "hash", Perfil.ATENDENTE, null);
        u.inativar();
        assertFalse(u.isAtivo());
        u.ativar();
        assertTrue(u.isAtivo());
    }

    @Test
    void perfil_marcaGrupoAdministrativo() {
        assertTrue(Perfil.GERENTE.isGrupoAdministrativo());
        assertTrue(Perfil.FINANCEIRO.isGrupoAdministrativo());
        assertFalse(Perfil.ATENDENTE.isGrupoAdministrativo());
    }
}
