package br.ufs.ziploc.devolucao.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.service.DevolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/devolucoes")
@RequiredArgsConstructor 
public class DevolucaoController {

    private final DevolucaoService devolucaoService;
    private final LocadoraApi locadoraApi;
    
    @PostMapping
    @PreAuthorize("hasRole('ATENDENTE')")
    public ResponseEntity<DevolucaoDto> registrar(@Valid @RequestBody RegistrarDevolucaoDto request, Authentication authentication) {
       
        Long locadoraId = locadoraApi.obterParametros().id();
        Long usuarioId = Long.valueOf(authentication.getName());
        
        DevolucaoDto devolucaoDto = devolucaoService.registrar(request,locadoraId,usuarioId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucaoDto);
    }
    

}
