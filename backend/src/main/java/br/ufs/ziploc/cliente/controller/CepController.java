package br.ufs.ziploc.cliente.controller;

import br.ufs.ziploc.cliente.dto.CepResponseDTO;
import br.ufs.ziploc.cliente.service.CepInvalidoException;
import br.ufs.ziploc.cliente.service.CepNaoEncontradoException;
import br.ufs.ziploc.cliente.service.ConsultaCepService;
import br.ufs.ziploc.integracao.cep.CepIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/clientes/cep/{cep}. Fica numa classe separada do ClienteController
 * (que é da tarefa 7) só para as duas tarefas não editarem o mesmo arquivo;
 * pode ser fundida depois. Sem regra aqui: o service faz tudo.
 */
@RestController
@RequestMapping("/api/clientes/cep")
@RequiredArgsConstructor
public class CepController {

    private final ConsultaCepService consultaCepService;

    @GetMapping("/{cep}")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public CepResponseDTO consultar(@PathVariable String cep) {
        return consultaCepService.consultar(cep);
    }

    // PROVISÓRIO: sai quando existir o @RestControllerAdvice único do shared (seção 11.1).
    @ExceptionHandler(CepInvalidoException.class)
    public ProblemDetail cepInvalido(CepInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CepNaoEncontradoException.class)
    public ProblemDetail cepNaoEncontrado(CepNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CepIndisponivelException.class)
    public ProblemDetail cepIndisponivel(CepIndisponivelException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                e.getMessage() + ": digite o endereço manualmente");
    }
}
