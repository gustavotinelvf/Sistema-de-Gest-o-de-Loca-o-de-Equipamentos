package br.ufs.ziploc.shared.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";

    private final JwtService jwtService = new JwtService(SEGREDO, 120);

    @Test
    void gerarEValidar_devolveIdEPerfil() {
        JwtService.TokenEmitido emitido = jwtService.gerar(7L, "TECNICO");

        assertNotNull(emitido.token());
        assertTrue(emitido.expiraEm().isAfter(Instant.now()));
        JwtService.DadosToken dados = jwtService.validar(emitido.token()).orElseThrow();
        assertEquals(7L, dados.usuarioId());
        assertEquals("TECNICO", dados.perfil());
    }

    @Test
    void validar_tokenAdulterado_devolveVazio() {
        String token = jwtService.gerar(7L, "TECNICO").token();
        assertTrue(jwtService.validar(token + "x").isEmpty());
    }

    @Test
    void validar_tokenExpirado_devolveVazio() {
        JwtService expirado = new JwtService(SEGREDO, -1);
        String token = expirado.gerar(7L, "TECNICO").token();
        assertTrue(jwtService.validar(token).isEmpty());
    }

    @Test
    void validar_tokenDeOutraChave_devolveVazio() {
        JwtService outro = new JwtService("outro-segredo-de-teste-com-mais-de-32-caracteres", 120);
        String token = outro.gerar(7L, "TECNICO").token();
        assertTrue(jwtService.validar(token).isEmpty());
    }

    @Test
    void validar_textoQualquer_devolveVazio() {
        assertTrue(jwtService.validar("isso-nao-e-um-jwt").isEmpty());
    }
}
