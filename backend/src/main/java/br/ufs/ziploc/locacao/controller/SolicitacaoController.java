package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.locacao.dto.ReprovacaoRequest;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.service.SolicitacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {
    private final SolicitacaoService solicitacaoService;

    @PostMapping("/{id}/reprovacao")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<SolicitacaoDTO> reprovar(@PathVariable Long id, @Valid @RequestBody ReprovacaoRequest request, Authentication autenticacao) {
        return ResponseEntity.ok(solicitacaoService.reprovar(id, Long.valueOf(autenticacao.getName()), request.justificativa()));
    }
}
