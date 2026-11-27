package br.ufs.ziploc.shared.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";

    private final JwtService jwtService = new JwtService(SEGREDO, 120);
    private final UsuarioApi usuarioApi = mock(UsuarioApi.class);
    private final JwtAuthenticationFilter filtro =
            new JwtAuthenticationFilter(jwtService, usuarioApi);

    @BeforeEach
    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValido_usuarioAtivo_autentica() throws Exception {
        when(usuarioApi.consultarPerfilAtivo(7L))
                .thenReturn(Optional.of("GERENTE"));

        executar(jwtService.gerar(7L, "GERENTE").token());

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(autenticacao);
        assertEquals("7", autenticacao.getName());
        assertEquals("ROLE_GERENTE",
                autenticacao.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void tokenValido_usuarioSemAcesso_naoAutentica() throws Exception {
        when(usuarioApi.consultarPerfilAtivo(7L))
                .thenReturn(Optional.empty());

        executar(jwtService.gerar(7L, "GERENTE").token());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValido_perfilAlterado_utilizaPerfilAtual() throws Exception {
        when(usuarioApi.consultarPerfilAtivo(7L))
                .thenReturn(Optional.of("ATENDENTE"));

        executar(jwtService.gerar(7L, "GERENTE").token());

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(autenticacao);
        assertEquals("ROLE_ATENDENTE",
                autenticacao.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void tokenInvalido_naoAutentica() throws Exception {
        executar("token-invalido");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioApi);
    }

    @Test
    void tokenExpirado_naoAutentica() throws Exception {
        JwtService expirado = new JwtService(SEGREDO, -1);

        executar(expirado.gerar(7L, "GERENTE").token());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioApi);
    }

    @Test
    void semToken_naoAutentica() throws Exception {
        executar(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioApi);
    }

    private void executar(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }

        filtro.doFilter(request, response, new MockFilterChain());
    }
}