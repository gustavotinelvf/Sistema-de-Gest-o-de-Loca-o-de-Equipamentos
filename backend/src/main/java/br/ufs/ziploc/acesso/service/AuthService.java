package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.dto.LoginRequestDTO;
import br.ufs.ziploc.acesso.dto.TokenResponseDTO;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import br.ufs.ziploc.shared.seguranca.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC13 - Autenticar Usuário. A regra de negócio do login vive aqui (seção 11). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public TokenResponseDTO autenticar(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .filter(Usuario::isAtivo)
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        JwtService.TokenEmitido emitido =
                jwtService.gerar(usuario.getId(), usuario.getPerfil().name());
        return new TokenResponseDTO(emitido.token(), usuario.getPerfil(), emitido.expiraEm());
    }
}
