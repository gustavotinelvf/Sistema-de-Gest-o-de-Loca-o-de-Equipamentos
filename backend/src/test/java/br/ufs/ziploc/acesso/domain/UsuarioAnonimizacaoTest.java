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

    private static final Instant INATIVACAO = Instant.parse("2025-01-01T12:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");

    private Usuario inativo(long id, Perfil perfil, String cpf) {
        Usuario u = Usuario.criarAtivo(1L, "Ana Souza", "ana@x.com", "hash", perfil, cpf);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "cargo", "Atendente sênior");
        u.inativar(INATIVACAO);
        return u;
    }

    @Test
    void anonimizar_inativo_removeDadosPessoaisECredenciais() {
        Usuario u = inativo(7L, Perfil.GERENTE, "12345678901");

        u.anonimizar(AGORA);

        assertTrue(u.isAnonimizado());
        assertEquals(AGORA, u.getAnonimizadoEm());
        assertEquals("ANONIMIZADO", u.getNome());
        assertEquals("anonimizado-7@anonimizado.invalid", u.getEmail());
        assertNull(u.getCpf());
        assertNull(u.getSenhaHash());
        assertNull(u.getCargo());
    }

    @Test
    void anonimizar_mantemInativoEPerfil_paraRespeitarOsChecksDoBanco() {
        Usuario u = inativo(8L, Perfil.GERENTE, "12345678901");

        u.anonimizar(AGORA);

        assertEquals(StatusUsuario.INATIVO, u.getStatus());
        assertEquals(Perfil.GERENTE, u.getPerfil());
        assertFalse(u.isAtivo());
    }

    @Test
    void anonimizar_emailsGeradosSaoDistintosParaIdsDistintos() {
        Usuario a = inativo(1L, Perfil.ATENDENTE, null);
        Usuario b = inativo(2L, Perfil.ATENDENTE, null);

        a.anonimizar(AGORA);
        b.anonimizar(AGORA);

        assertFalse(a.getEmail().equalsIgnoreCase(b.getEmail()));
    }

    @Test
    void anonimizar_usuarioAtivo_rejeita() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "id", 3L);

        IllegalStateException erro =
                assertThrows(IllegalStateException.class, () -> u.anonimizar(AGORA));

        assertEquals("Só um usuário INATIVO pode ser anonimizado", erro.getMessage());
        assertEquals("Ana", u.getNome());
        assertFalse(u.isAnonimizado());
    }

    @Test
    void anonimizar_usuarioJaAnonimizado_rejeita() {
        Usuario u = inativo(4L, Perfil.ATENDENTE, null);
        u.anonimizar(AGORA);

        IllegalStateException erro =
                assertThrows(IllegalStateException.class,
                        () -> u.anonimizar(AGORA.plusSeconds(86400)));

        assertEquals("O usuário já foi anonimizado", erro.getMessage());
        assertEquals(AGORA, u.getAnonimizadoEm());
    }

    @Test
    void anonimizar_semId_rejeita() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.ATENDENTE, null);
        u.inativar(INATIVACAO);

        assertThrows(IllegalStateException.class, () -> u.anonimizar(AGORA));
        assertFalse(u.isAnonimizado());
    }

    @Test
    void anonimizar_semData_rejeita() {
        Usuario u = inativo(5L, Perfil.ATENDENTE, null);

        assertThrows(NullPointerException.class, () -> u.anonimizar(null));
        assertFalse(u.isAnonimizado());
    }

    @Test
    void anonimizado_naoPodeSerReativado() {
        Usuario u = inativo(6L, Perfil.ATENDENTE, null);
        u.anonimizar(AGORA);

        assertThrows(IllegalStateException.class, u::ativar);
        assertEquals(StatusUsuario.INATIVO, u.getStatus());
        assertTrue(u.isAnonimizado());
    }

    @Test
    void anonimizar_usuarioPendente_rejeita() {
        Usuario u = Usuario.criarPendente(
                1L, "Ana", "ana@x.com", Perfil.ATENDENTE, null);

        ReflectionTestUtils.setField(u, "id", 9L);

        assertThrows(IllegalStateException.class, () -> u.anonimizar(AGORA));

        assertEquals(StatusUsuario.PENDENTE, u.getStatus());
        assertEquals("Ana", u.getNome());
        assertFalse(u.isAnonimizado());
    }

    @Test
    void anonimizar_preservaIdentidadeReferencialEHistorico() {
        Usuario u = inativo(10L, Perfil.FINANCEIRO, null);

        Instant cadastro = Instant.parse("2024-01-01T12:00:00Z");
        Instant ativacao = Instant.parse("2024-01-02T12:00:00Z");

        ReflectionTestUtils.setField(u, "dataCadastro", cadastro);
        ReflectionTestUtils.setField(u, "dataAtivacao", ativacao);

        u.anonimizar(AGORA);

        assertEquals(10L, u.getId());
        assertEquals(1L, u.getLocadoraId());
        assertEquals(Perfil.FINANCEIRO, u.getPerfil());
        assertEquals(cadastro, u.getDataCadastro());
        assertEquals(ativacao, u.getDataAtivacao());
        assertEquals(INATIVACAO, u.getDataInativacao());
        assertEquals(AGORA, u.getAnonimizadoEm());
    }

    @Test
    void anonimizar_semDataDeInativacao_rejeita() {
        Usuario u = inativo(11L, Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "dataInativacao", null);

        assertThrows(IllegalStateException.class, () -> u.anonimizar(AGORA));

        assertFalse(u.isAnonimizado());
        assertEquals("Ana Souza", u.getNome());
        assertEquals("ana@x.com", u.getEmail());
    }

    @Test
    void anonimizar_antesDaInativacao_rejeita() {
        Usuario u = inativo(12L, Perfil.ATENDENTE, null);

        assertThrows(IllegalArgumentException.class,
                () -> u.anonimizar(INATIVACAO.minusSeconds(1)));

        assertFalse(u.isAnonimizado());
        assertEquals("Ana Souza", u.getNome());
        assertEquals("hash", u.getSenhaHash());
    }

    @Test
    void anonimizar_aposReativacao_rejeita() {
        Usuario u = inativo(13L, Perfil.ATENDENTE, null);

        u.ativar();

        assertThrows(IllegalStateException.class, () -> u.anonimizar(AGORA));

        assertTrue(u.isAtivo());
        assertFalse(u.isAnonimizado());
        assertNull(u.getDataInativacao());
    }

    @Test
    void anonimizar_novaInativacaoNaoAlteraRegistroAnonimizado() {
        Usuario u = inativo(14L, Perfil.ATENDENTE, null);
        u.anonimizar(AGORA);

        u.inativar(AGORA.plusSeconds(86400));

        assertEquals(INATIVACAO, u.getDataInativacao());
        assertEquals(AGORA, u.getAnonimizadoEm());
        assertEquals(StatusUsuario.INATIVO, u.getStatus());
        assertEquals("anonimizado-14@anonimizado.invalid", u.getEmail());
        assertNull(u.getSenhaHash());
    }

    @Test
    void criarAtivo_comDominioReservado_rejeita() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarAtivo(
                        1L, "Ana", "anonimizado-7@anonimizado.invalid",
                        "hash", Perfil.ATENDENTE, null));
    }

    @Test
    void criarPendente_comDominioReservado_rejeita() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarPendente(
                        1L, "Ana", "anonimizado-7@anonimizado.invalid",
                        Perfil.ATENDENTE, null));
    }

    @Test
    void criarAtivo_comDominioReservadoEmMaiusculas_rejeita() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarAtivo(
                        1L, "Ana", "anonimizado-7@ANONIMIZADO.INVALID",
                        "hash", Perfil.ATENDENTE, null));
    }

    @Test
    void criarPendente_comDominioReservadoEEspacos_rejeita() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.criarPendente(
                        1L, "Ana", " anonimizado-7@anonimizado.invalid ",
                        Perfil.ATENDENTE, null));
    }
}