package br.ufs.ziploc.faturamento.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.service.FaturaService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;



@RestController 
@RequestMapping("/api/faturas")
@RequiredArgsConstructor

public class FaturaController {

    private final FaturaService faturaService;

    @PostMapping("/{id}/pagamento")
    @PreAuthorize("hasAnyRole('FINANCEIRO','GERENTE')")
    public ResponseEntity<FaturaDto> registrar(@PathVariable Long id) {

        Long locadoraId = 1L;
        Long usuarioId = 1L;
        /*TODO: Preciso desses dados validados. Dúvida em relação a isto, como conseguir esses dados? Vem das outras equipes */
        FaturaDto faturaDto = faturaService.registrarPagamento(id,locadoraId,usuarioId);
        return ResponseEntity.ok(faturaDto);
    }
    
}
