package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.locacao.dto.ContratoResponse;
import br.ufs.ziploc.locacao.dto.JustificativaRequest;
import br.ufs.ziploc.locacao.dto.ProrrogacaoRequest;
import br.ufs.ziploc.locacao.dto.RetiradaRequest;
import br.ufs.ziploc.locacao.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE', 'FINANCEIRO')")
    public ResponseEntity<ContratoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping("/{id}/retirada")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<ContratoResponse> retirar(@PathVariable Long id, @RequestBody(required = false) RetiradaRequest request, Authentication autenticacao) {
        return ResponseEntity.ok(service.retirar(id, request == null ? null : request.dataRetirada(), Long.valueOf(autenticacao.getName())));
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<ContratoResponse> cancelar(@PathVariable Long id, @Valid @RequestBody JustificativaRequest request) {
        return ResponseEntity.ok(service.cancelar(id, request.justificativa()));
    }

    @PostMapping("/{id}/prorrogacao")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<ContratoResponse> prorrogar(@PathVariable Long id, @Valid @RequestBody ProrrogacaoRequest request) {
        return ResponseEntity.ok(service.prorrogar(id, request.novaDataFim()));
    }
}