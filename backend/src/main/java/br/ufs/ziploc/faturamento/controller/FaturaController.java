package br.ufs.ziploc.faturamento.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.service.FaturaService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/faturas")
@RequiredArgsConstructor

public class FaturaController {

    private final FaturaService faturaService;
    private final LocadoraApi locadoraApi;

    @PostMapping("/{id}/pagamento")
    @PreAuthorize("hasRole('FINANCEIRO')")
    public ResponseEntity<FaturaDto> registrar(@PathVariable Long id, Authentication autenticacao) {

        Long locadoraId = locadoraApi.obterParametros().id();
        Long usuarioId = Long.valueOf(autenticacao.getName());

        FaturaDto faturaDto = faturaService.registrarPagamento(id,locadoraId,usuarioId);
        return ResponseEntity.ok(faturaDto);
    }
}