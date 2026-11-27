package br.ufs.ziploc.acesso.controller;

import br.ufs.ziploc.acesso.dto.UsuarioResponseDTO;
import br.ufs.ziploc.acesso.service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só o Gerente gerencia usuários. Sem regra aqui (seção 11). */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('GERENTE')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar();
    }

    @PostMapping("/{id}/inativacao")
    public UsuarioResponseDTO inativar(@PathVariable Long id, Authentication autenticacao) {
        return usuarioService.inativar(id, Long.valueOf(autenticacao.getName()));
    }

    @PostMapping("/{id}/reativacao")
    public UsuarioResponseDTO reativar(@PathVariable Long id) {
        return usuarioService.reativar(id);
    }
}
