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
    public ResponseEntity<FaturaDto> registrar(@PathVariable Long id, @RequestParam Long locadoraId, @RequestParam Long usuarioId) {

        /*Dúvida sobre aplicação correta destes requestParam. Como eu autorizei ao financeiro, não sei se fiz correto o acesso */
        /*Verificar essa parte do acesso aos parâmetros locadoraId e usuarioId se foram feitos corretamente */
        FaturaDto faturaDto = faturaService.registrarPagamento(id,locadoraId,usuarioId);
        return ResponseEntity.ok(faturaDto);
    }
    
}
