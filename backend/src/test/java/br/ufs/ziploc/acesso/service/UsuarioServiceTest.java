package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.StatusUsuario;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.dto.UsuarioResponseDTO;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuario(long id, String nome) {
        Usuario u = Usuario.criarAtivo(1L, nome, nome.toLowerCase() + "@x.com", "hash", Perfil.ATENDENTE, null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void listar_devolveTodosOrdenadosPorNomeSemSenha() {
        when(usuarioRepository.findAll(Sort.by("nome")))
                .thenReturn(List.of(usuario(1L, "Ana"), usuario(2L, "Bia")));

        List<UsuarioResponseDTO> lista = service.listar();

        assertEquals(2, lista.size());
        assertEquals("Ana", lista.get(0).nome());
        assertEquals(StatusUsuario.ATIVO, lista.get(0).status());
    }

    @Test
    void inativar_outroUsuario_ficaInativo() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "Bia")));

        UsuarioResponseDTO dto = service.inativar(2L, 1L);

        assertEquals(StatusUsuario.INATIVO, dto.status());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void inativar_aPropriaConta_lancaOperacaoInvalida() {
        assertThrows(OperacaoUsuarioInvalidaException.class, () -> service.inativar(1L, 1L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void inativar_usuarioInexistente_lancaNaoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontradoException.class, () -> service.inativar(9L, 1L));
    }

    @Test
    void reativar_usuarioInativo_voltaAtivo() {
        Usuario inativo = usuario(2L, "Bia");
        inativo.inativar();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(inativo));

        UsuarioResponseDTO dto = service.reativar(2L);

        assertEquals(StatusUsuario.ATIVO, dto.status());
    }

    @Test
    void reativar_usuarioQueNuncaAceitouConvite_lancaOperacaoInvalida() {
        Usuario pendente = Usuario.criarPendente(1L, "Caio", "caio@x.com", Perfil.ESTOQUISTA, null);
        ReflectionTestUtils.setField(pendente, "id", 3L);
        pendente.inativar();
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(pendente));

        assertThrows(OperacaoUsuarioInvalidaException.class, () -> service.reativar(3L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reativar_usuarioInexistente_lancaNaoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontradoException.class, () -> service.reativar(9L));
    }

    @Test
    void reativar_usuarioAnonimizado_lancaOperacaoInvalida() {
        Usuario anonimizado = usuario(4L, "Usuario Anonimizado");
        anonimizado.inativar();

        ReflectionTestUtils.setField(anonimizado, "anonimizadoEm", Instant.now());
        ReflectionTestUtils.setField(anonimizado, "senhaHash", null);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(anonimizado));

        OperacaoUsuarioInvalidaException erro = assertThrows(
                OperacaoUsuarioInvalidaException.class,
                () -> service.reativar(4L));

        assertEquals("Um usuário anonimizado não pode ser reativado", erro.getMessage());
        assertEquals(StatusUsuario.INATIVO, anonimizado.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reativar_gerenteSemCpf_lancaOperacaoInvalida() {
        Usuario gerente = Usuario.criarAtivo(
                1L, "Ana", "ana@x.com", "hash", Perfil.GERENTE, "12345678901");

        ReflectionTestUtils.setField(gerente, "id", 5L);
        gerente.inativar();
        ReflectionTestUtils.setField(gerente, "cpf", null);

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(gerente));

        OperacaoUsuarioInvalidaException erro = assertThrows(
                OperacaoUsuarioInvalidaException.class,
                () -> service.reativar(5L));

        assertEquals("CPF é obrigatório para o perfil GERENTE (RD016)", erro.getMessage());
        assertEquals(StatusUsuario.INATIVO, gerente.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void consultarPerfilAtivo_usuarioAtivo_retornaPerfil() {
        Usuario ativo = usuario(1L, "Ana");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ativo));

        assertEquals(Optional.of("ATENDENTE"), service.consultarPerfilAtivo(1L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void consultarPerfilAtivo_usuarioInativo_retornaVazio() {
        Usuario inativo = usuario(2L, "Bia");
        inativo.inativar();

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(inativo));

        assertTrue(service.consultarPerfilAtivo(2L).isEmpty());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void consultarPerfilAtivo_usuarioPendente_retornaVazio() {
        Usuario pendente = Usuario.criarPendente(
                1L, "Caio", "caio@x.com", Perfil.ESTOQUISTA, null);

        ReflectionTestUtils.setField(pendente, "id", 3L);

        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(pendente));

        assertTrue(service.consultarPerfilAtivo(3L).isEmpty());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void consultarPerfilAtivo_usuarioAnonimizado_retornaVazio() {
        Usuario anonimizado = usuario(4L, "Usuario Anonimizado");

        anonimizado.inativar();
        ReflectionTestUtils.setField(anonimizado, "anonimizadoEm", Instant.now());
        ReflectionTestUtils.setField(anonimizado, "senhaHash", null);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(anonimizado));

        assertTrue(service.consultarPerfilAtivo(4L).isEmpty());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void consultarPerfilAtivo_usuarioInexistente_retornaVazio() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertTrue(service.consultarPerfilAtivo(9L).isEmpty());
        verify(usuarioRepository, never()).save(any());
    }
}
