package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.dto.UsuarioResponseDTO;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de usuários pelo Gerente: listar, inativar e reativar. */
@Service
@RequiredArgsConstructor
public class UsuarioService implements UsuarioApi {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> consultarPerfilAtivo(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .filter(usuario -> !usuario.isAnonimizado())
                .map(usuario -> usuario.getPerfil().name());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll(Sort.by("nome")).stream()
                .map(UsuarioResponseDTO::from)
                .toList();
    }

    /** Regra sugerida pela equipe 1: ninguém inativa a própria conta (evita ficar sem acesso). */
    @Transactional
    public UsuarioResponseDTO inativar(Long id, Long solicitanteId) {
        if (id.equals(solicitanteId)) {
            throw new OperacaoUsuarioInvalidaException("Você não pode inativar a própria conta");
        }
        Usuario usuario = buscar(id);
        usuario.inativar();
        usuarioRepository.save(usuario);
        return UsuarioResponseDTO.from(usuario);
    }

    @Transactional
    public UsuarioResponseDTO reativar(Long id) {
        Usuario usuario = buscar(id);

        try {
            usuario.ativar();
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new OperacaoUsuarioInvalidaException(e.getMessage());
        }

        usuarioRepository.save(usuario);
        return UsuarioResponseDTO.from(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}