package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.service.SolicitacaoNaoEncontradaException;
import br.ufs.ziploc.shared.persistence.ViolacaoIntegridade;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice(basePackages = "br.ufs.ziploc.locacao")
public class LocacaoExceptionHandler {
    @ExceptionHandler({SolicitacaoNaoEncontradaException.class, ContratoNaoEncontradoException.class})
    public ProblemDetail naoEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IndisponibilidadeException.class)
    public ProblemDetail indisponivel(IndisponibilidadeException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setProperty("faltas", e.getFaltas());
        return problema;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail reservaSobreposta(DataIntegrityViolationException e) {
        if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.EXCLUSAO)) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Uma das unidades já está reservada no período.");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A operação viola uma regra de integridade dos dados.");
    }
}