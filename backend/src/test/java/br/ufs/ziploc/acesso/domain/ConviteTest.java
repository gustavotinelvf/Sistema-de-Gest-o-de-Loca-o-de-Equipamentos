package br.ufs.ziploc.acesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConviteTest {

    private static Usuario destinatario() {
        return Usuario.criarPendente(1L, "Novo", "novo@x.com", Perfil.ATENDENTE, null);
    }

    private Convite novoConvite() {
        return Convite.criar(destinatario(), Convite.hashDoToken("token-1"),
                Instant.now().plus(Duration.ofDays(2)));
    }

    @Test
    void criar_comecaPendente() {
        assertEquals(StatusConvite.PENDENTE, novoConvite().getStatus());
    }

    @Test
    void criar_usuarioQueNaoEstaPendente_rejeita() {
        Usuario ativo = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash", Perfil.ATENDENTE, null);
        assertThrows(IllegalArgumentException.class, () -> Convite.criar(ativo,
                Convite.hashDoToken("t"), Instant.now().plus(Duration.ofDays(1))));
    }

    @Test
    void criar_hashForaDoFormato_rejeita() {
        assertThrows(IllegalArgumentException.class, () -> Convite.criar(destinatario(), "curto",
                Instant.now().plus(Duration.ofDays(1))));
    }

    @Test
    void criar_expiracaoNoPassado_rejeita_RD017() {
        assertThrows(IllegalArgumentException.class, () -> Convite.criar(destinatario(),
                Convite.hashDoToken("t"), Instant.now().minusSeconds(60)));
    }

    @Test
    void criar_expiracaoNula_rejeita() {
        assertThrows(NullPointerException.class, () -> Convite.criar(destinatario(),
                Convite.hashDoToken("t"), null));
    }

    @Test
    void hashDoToken_tem64HexadecimaisEDependeDoToken() {
        String h1 = Convite.hashDoToken("abc");
        assertTrue(h1.matches("[0-9a-f]{64}"));
        assertEquals(h1, Convite.hashDoToken("abc"));
        assertNotEquals(h1, Convite.hashDoToken("abd"));
    }

    @Test
    void aceitar_pendente_viraAceito() {
        Convite c = novoConvite();
        c.aceitar(Instant.now());
        assertEquals(StatusConvite.ACEITO, c.getStatus());
    }

    @Test
    void aceitar_registraDataDeAceite() {
        Convite c = novoConvite();
        Instant agora = Instant.now();
        c.aceitar(agora);
        assertEquals(agora, c.getDataAceite());
    }

    @Test
    void aceitar_conviteCancelado_rejeita_RD017() {
        Convite c = novoConvite();
        c.cancelar();
        assertThrows(IllegalStateException.class, () -> c.aceitar(Instant.now()));
    }

    @Test
    void aceitar_conviteVencido_rejeita_RD017() {
        Convite c = novoConvite();
        Instant depoisDoVencimento = Instant.now().plus(Duration.ofDays(3));
        assertThrows(IllegalStateException.class, () -> c.aceitar(depoisDoVencimento));
        assertEquals(StatusConvite.PENDENTE, c.getStatus());
    }

    @Test
    void expirar_so_se_pendente() {
        Convite c = novoConvite();
        c.expirar();
        assertEquals(StatusConvite.EXPIRADO, c.getStatus());
        assertThrows(IllegalStateException.class, c::expirar);
    }

    @Test
    void isVencido_comparaComDataDeExpiracao() {
        Convite c = novoConvite();
        assertFalse(c.isVencido(Instant.now()));
        assertTrue(c.isVencido(Instant.now().plus(Duration.ofDays(3))));
    }

    @Test
    void isVencido_noInstanteExatoDaExpiracao() {
        Convite c = novoConvite();
        assertTrue(c.isVencido(c.getDataExpiracao()));
    }
}
