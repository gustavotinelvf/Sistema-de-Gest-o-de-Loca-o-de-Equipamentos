package br.ufs.ziploc.acesso.controller;

import br.ufs.ziploc.acesso.dto.LoginRequestDTO;
import br.ufs.ziploc.acesso.dto.TokenResponseDTO;
import br.ufs.ziploc.acesso.service.AuthService;
import br.ufs.ziploc.acesso.service.CredenciaisInvalidasException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota pública (sem token): só valida o formato e chama o service. Sem regra aqui. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> autenticar(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.autenticar(dto));
    }

    // PROVISÓRIO: quando o @RestControllerAdvice único do shared existir (seção 11.1),
    // este método sai daqui e a tradução para 401 passa a ser feita lá.
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}
