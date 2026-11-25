package br.ufs.ziploc.vistoria.controller;

import br.ufs.ziploc.vistoria.dto.RegistrarVistoriaRequest;
import br.ufs.ziploc.vistoria.dto.VistoriaDto;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devolucoes/{devolucaoId}/vistoria")
@RequiredArgsConstructor
public class VistoriaController {

    private final VistoriaService vistoriaService;

    @PostMapping
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<VistoriaDto> registrar(@PathVariable("devolucaoId") Long devolucaoId,
                                                 @Valid @RequestBody RegistrarVistoriaRequest request,
                                                 Authentication autenticacao) {
        /* TODO: locadoraId provisório, igual ao DevolucaoController, até a segurança fornecer a locadora do usuário. */
        Long locadoraId = 1L;

        VistoriaDto criada = vistoriaService.registrar(locadoraId, devolucaoId, request.resultado(),
                request.descricaoAvarias(), Long.valueOf(autenticacao.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }
}