package br.ufs.ziploc.devolucao.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.service.DevolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/devolucoes")
@RequiredArgsConstructor 
public class DevolucaoController {

    private final DevolucaoService devolucaoService;
    
    @PostMapping
    public ResponseEntity<DevolucaoDto> registrar(@Valid @RequestBody RegistrarDevolucaoDto request) {
        /*TODO: @PreAuthorize quando já tiver o Atendente */
        /*TODO: Necessito do acesso ao locadoraId e o usuarioId quando a segurança destas partes estiver pronta. Isto é provisório */
        Long locadoraId = 1l; 
        Long usuarioId = 1l;
        DevolucaoDto devolucaoDto = devolucaoService.registrar(request,locadoraId , usuarioId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucaoDto);
    }
    

}
