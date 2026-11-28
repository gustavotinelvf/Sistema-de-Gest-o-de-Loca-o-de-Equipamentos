package br.ufs.ziploc.cliente.controller;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.cliente.dto.RegistrarReconsideracaoDTO;
import br.ufs.ziploc.cliente.dto.ReconsideracaoResponseDTO;
import br.ufs.ziploc.cliente.service.ReconsideracaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes/{clienteId}/reconsideracoes")
@RequiredArgsConstructor
public class ReconsideracaoController {

    private final ReconsideracaoService reconsideracaoService;

    private final LocadoraApi locadoraApi;

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<ReconsideracaoResponseDTO> registrar(
            @PathVariable Long clienteId,
            @Valid @RequestBody RegistrarReconsideracaoDTO request,
            Authentication autenticacao) {

        Long locadoraId =
                locadoraApi.obterParametros().id();

        Long gerenteId =
                Long.valueOf(autenticacao.getName());

        ReconsideracaoResponseDTO response =
                reconsideracaoService.registrar(
                        clienteId,
                        locadoraId,
                        gerenteId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}