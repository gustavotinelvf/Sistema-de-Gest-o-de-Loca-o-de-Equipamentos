package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.locacao.dto.AprovacaoRequest;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import br.ufs.ziploc.locacao.dto.JustificativaRequest;
import br.ufs.ziploc.locacao.dto.RegistrarSolicitacaoRequest;
import br.ufs.ziploc.locacao.dto.ReprovacaoRequest;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoResponse;
import br.ufs.ziploc.locacao.service.SolicitacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<SolicitacaoResponse> registrar(@Valid @RequestBody RegistrarSolicitacaoRequest request, Authentication autenticacao) {
        SolicitacaoResponse criada = solicitacaoService.registrar(request, Long.valueOf(autenticacao.getName()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(criada.id())).body(criada);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<SolicitacaoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitacaoService.buscar(id));
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<SolicitacaoResponse> cancelar(@PathVariable Long id, @Valid @RequestBody JustificativaRequest request) {
        return ResponseEntity.ok(solicitacaoService.cancelar(id, request.justificativa()));
    }

    @PostMapping("/{id}/aprovacao")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<ContratoDTO> aprovar(@PathVariable Long id, @Valid @RequestBody AprovacaoRequest request, Authentication autenticacao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitacaoService.aprovar(id, Long.valueOf(autenticacao.getName()), request.percentualMulta(), request.condicoesDevolucao()));
    }

    @PostMapping("/{id}/reprovacao")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<SolicitacaoDTO> reprovar(@PathVariable Long id, @Valid @RequestBody ReprovacaoRequest request, Authentication autenticacao) {
        return ResponseEntity.ok(solicitacaoService.reprovar(id, Long.valueOf(autenticacao.getName()), request.justificativa()));
    }
}