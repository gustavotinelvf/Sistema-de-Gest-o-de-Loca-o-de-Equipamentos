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

    // NOVO: campos obrigatórios nulos falham logo na criação.
    @Test
    void criar_expiracaoNula_rejeita() {
        Usuario g = Usuario.criarAtivo("Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");
        assertThrows(NullPointerException.class, () -> Convite.criar("n@x.com", Perfil.ATENDENTE, g,
                "t", null));
    }

    @Test
    void aceitar_pendente_viraAceito() {
        Convite c = novoConvite();
        c.aceitar(Instant.now());
        assertEquals(StatusConvite.ACEITO, c.getStatus());
    }

    // NOVO: registra a data do aceite (apagar se a coluna data_aceite não existir).
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

    // NOVO: convite PENDENTE já vencido não pode ser aceito (RD017).
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

    // NOVO: no instante exato da expiração o convite já está vencido.
    @Test
    void isVencido_noInstanteExatoDaExpiracao() {
        Convite c = novoConvite();
        assertTrue(c.isVencido(c.getDataExpiracao()));
    }
}