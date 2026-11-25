package br.ufs.ziploc.acesso.dto;

import br.ufs.ziploc.acesso.domain.Perfil;
import java.time.Instant;

/** Resposta do login: o token, o perfil e quando ele expira. */
public record TokenResponseDTO(String token, Perfil perfil, Instant expiraEm) {
}
