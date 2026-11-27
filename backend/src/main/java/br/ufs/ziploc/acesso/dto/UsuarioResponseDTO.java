package br.ufs.ziploc.acesso.dto;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.StatusUsuario;
import br.ufs.ziploc.acesso.domain.Usuario;
import java.time.Instant;

/** Dados de um usuário para a tela de gestão. Sem senha e sem CPF (LGPD: só o necessário). */
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        StatusUsuario status,
        Instant dataCadastro) {

    public static UsuarioResponseDTO from(Usuario u) {
        return new UsuarioResponseDTO(u.getId(), u.getNome(), u.getEmail(),
                u.getPerfil(), u.getStatus(), u.getDataCadastro());
    }
}
