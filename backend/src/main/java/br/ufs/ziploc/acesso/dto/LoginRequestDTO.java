package br.ufs.ziploc.acesso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Entrada do login. O @Valid do controller confere o formato (400). */
public record LoginRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String senha) {
}
