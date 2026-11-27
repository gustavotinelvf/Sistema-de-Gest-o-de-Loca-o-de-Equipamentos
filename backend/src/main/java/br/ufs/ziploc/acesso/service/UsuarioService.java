package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.dto.UsuarioResponseDTO;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de usuários pelo Gerente: listar, inativar e reativar. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

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
        // O banco exige senha para status ATIVO: quem nunca aceitou o convite não é reativado.
        if (usuario.getSenhaHash() == null) {
            throw new OperacaoUsuarioInvalidaException(
                    "O usuário ainda não concluiu o cadastro (convite pendente)");
        }
        usuario.ativar();
        usuarioRepository.save(usuario);
        return UsuarioResponseDTO.from(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
