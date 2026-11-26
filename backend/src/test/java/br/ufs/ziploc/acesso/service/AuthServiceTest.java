package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.dto.LoginRequestDTO;
import br.ufs.ziploc.acesso.dto.TokenResponseDTO;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import br.ufs.ziploc.shared.seguranca.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtService jwtService =
            new JwtService("segredo-de-teste-com-mais-de-32-caracteres!!", 120);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
    }

    private Usuario usuarioAtivo() {
        Usuario u = Usuario.criarAtivo(1L, "Ana", "ana@x.com", "hash-bcrypt", Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "id", 7L);
        return u;
    }

    @Test
    void autenticar_credenciaisValidas_retornaTokenComPerfil_UC13() {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuarioAtivo()));
        when(passwordEncoder.matches("senha123", "hash-bcrypt")).thenReturn(true);

        TokenResponseDTO resposta = authService.autenticar(new LoginRequestDTO("ana@x.com", "senha123"));

        assertEquals(Perfil.ATENDENTE, resposta.perfil());
        assertNotNull(resposta.token());
        assertTrue(resposta.expiraEm().isAfter(Instant.now()));
        JwtService.DadosToken dados = jwtService.validar(resposta.token()).orElseThrow();
        assertEquals(7L, dados.usuarioId());
        assertEquals("ATENDENTE", dados.perfil());
    }

    @Test
    void autenticar_emailInexistente_lancaCredenciaisInvalidas_UC13() {
        when(usuarioRepository.findByEmail("naoexiste@x.com")).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.autenticar(new LoginRequestDTO("naoexiste@x.com", "qualquer")));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void autenticar_usuarioInativo_lancaCredenciaisInvalidas_UC13() {
        Usuario inativo = usuarioAtivo();
        inativo.inativar();
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(inativo));

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@x.com", "senha123")));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void autenticar_senhaErrada_lancaCredenciaisInvalidas_UC13() {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuarioAtivo()));
        when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.autenticar(new LoginRequestDTO("ana@x.com", "errada")));
    }
}
