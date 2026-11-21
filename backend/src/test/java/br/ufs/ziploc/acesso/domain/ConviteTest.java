package br.ufs.ziploc.acesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConviteTest {

    private Convite novoConvite() {
        Usuario gerente = Usuario.criarAtivo("Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");
        return Convite.criar("novo@x.com", Perfil.ATENDENTE, gerente, "token-1",
                Instant.now().plus(Duration.ofDays(2)));
    }

    @Test
    void criar_comecaPendente() {
        assertEquals(StatusConvite.PENDENTE, novoConvite().getStatus());
    }

    @Test
    void criar_expiracaoNoPassado_rejeita_RD017() {
        Usuario g = Usuario.criarAtivo("Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");
        assertThrows(IllegalArgumentException.class, () -> Convite.criar("n@x.com", Perfil.ATENDENTE, g,
                "t", Instant.now().minusSeconds(60)));
    }

    @Test
    void aceitar_pendente_viraAceito() {
        Convite c = novoConvite();
        c.aceitar();
        assertEquals(StatusConvite.ACEITO, c.getStatus());
    }

    @Test
    void aceitar_conviteCancelado_rejeita_RD017() {
        Convite c = novoConvite();
        c.cancelar();
        assertThrows(IllegalStateException.class, c::aceitar);
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
}
