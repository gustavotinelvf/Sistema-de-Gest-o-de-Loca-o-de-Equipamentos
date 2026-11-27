package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.domain.EnderecoLocadora;
import br.ufs.ziploc.acesso.domain.Locadora;
import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.LocadoraRepository;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AcessoInicializadorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LocadoraRepository locadoraRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockEnvironment env;

    private AcessoInicializador inicializador;

    @BeforeEach
    void setUp() {
        env = new MockEnvironment()
                .withProperty("ADMIN_NOME", "Maria Gerente")
                .withProperty("ADMIN_EMAIL", "maria@ziploc.com")
                .withProperty("ADMIN_CPF", "123.456.789-01")
                .withProperty("ADMIN_SENHA", "Senha@123")
                .withProperty("LOCADORA_RAZAO_SOCIAL", "ZipLoc Ltda")
                .withProperty("LOCADORA_CNPJ", "12.345.678/0001-99")
                .withProperty("LOCADORA_CEP", "49000-000")
                .withProperty("LOCADORA_LOGRADOURO", "Rua A")
                .withProperty("LOCADORA_NUMERO", "10")
                .withProperty("LOCADORA_BAIRRO", "Centro")
                .withProperty("LOCADORA_CIDADE", "Aracaju")
                .withProperty("LOCADORA_UF", "se")
                .withProperty("LOCADORA_RAIO_ENTREGA_KM", "30")
                .withProperty("LOCADORA_PERCENTUAL_MULTA", "2.5");
        inicializador = new AcessoInicializador(usuarioRepository, locadoraRepository, passwordEncoder, env);
    }

    private void executar() {
        inicializador.run(new DefaultApplicationArguments());
    }

    @Test
    void run_jaExisteGerente_naoFazNada() {
        when(usuarioRepository.existsByPerfil(Perfil.GERENTE)).thenReturn(true);

        executar();

        verifyNoInteractions(locadoraRepository, passwordEncoder);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_semGerenteComVariaveis_criaGerenteELocadora() {
        when(usuarioRepository.existsByPerfil(Perfil.GERENTE)).thenReturn(false);
        when(locadoraRepository.findUnica()).thenReturn(Optional.empty());
        when(locadoraRepository.save(any(Locadora.class))).thenAnswer(inv -> {
            Locadora salva = inv.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 5L); // o banco gera o id (identity)
            return salva;
        });
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash-bcrypt");

        executar();

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertEquals(Perfil.GERENTE, usuario.getValue().getPerfil());
        assertEquals("maria@ziploc.com", usuario.getValue().getEmail());
        assertEquals("hash-bcrypt", usuario.getValue().getSenhaHash());
        assertEquals("12345678901", usuario.getValue().getCpf());
        assertEquals(5L, usuario.getValue().getLocadoraId());

        ArgumentCaptor<Locadora> locadora = ArgumentCaptor.forClass(Locadora.class);
        verify(locadoraRepository).save(locadora.capture());
        assertEquals("12345678000199", locadora.getValue().getCnpj());
        assertEquals("49000000", locadora.getValue().getEndereco().getCep());
        assertEquals("SE", locadora.getValue().getEndereco().getUf());
        assertNull(locadora.getValue().getEndereco().getComplemento());
        assertEquals(new BigDecimal("30"), locadora.getValue().getRaioEntregaKm());
        assertEquals(new BigDecimal("2.5"), locadora.getValue().getPercentualMultaPadrao());
    }

    @Test
    void run_locadoraJaExiste_criaSoOGerente() {
        when(usuarioRepository.existsByPerfil(Perfil.GERENTE)).thenReturn(false);
        var endereco = new EnderecoLocadora("49000000", "Rua A", "10", null, "Centro", "Aracaju", "SE");
        Locadora existente = new Locadora("ZipLoc Ltda", "12345678000199", endereco,
                new BigDecimal("30"), new BigDecimal("2.5"));
        ReflectionTestUtils.setField(existente, "id", 9L);
        when(locadoraRepository.findUnica()).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash-bcrypt");

        executar();

        verify(locadoraRepository, never()).save(any());
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertEquals(9L, usuario.getValue().getLocadoraId());
    }

    @Test
    void run_variavelAusente_naoCriaNada() {
        when(usuarioRepository.existsByPerfil(Perfil.GERENTE)).thenReturn(false);
        env.withProperty("ADMIN_SENHA", "");

        executar();

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(locadoraRepository, passwordEncoder);
    }
}
