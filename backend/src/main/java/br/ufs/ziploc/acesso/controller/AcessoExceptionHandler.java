package br.ufs.ziploc.acesso.controller;

import br.ufs.ziploc.acesso.service.OperacaoUsuarioInvalidaException;
import br.ufs.ziploc.acesso.service.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * PROVISÓRIO: traduz os erros do UsuarioController para ProblemDetail até existir
 * o @RestControllerAdvice único do shared (seção 11.1). Quando existir, apagar esta classe.
 */
@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class AcessoExceptionHandler {

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(UsuarioNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OperacaoUsuarioInvalidaException.class)
    public ProblemDetail conflito(OperacaoUsuarioInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
