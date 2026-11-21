package br.ufs.ziploc.manutencao.controller;

import br.ufs.ziploc.manutencao.dto.*;
import br.ufs.ziploc.manutencao.service.ManutencaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    private final ManutencaoService service;

    public ManutencaoController(ManutencaoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<ManutencaoDto> iniciar(@Valid @RequestBody IniciarManutencaoRequest req,
                                                 Authentication auth) {
        ManutencaoDto criada = service.iniciarAvulsa(
                req.numeroSerie(), req.descricaoProblema(), idDoUsuario(auth));
        return ResponseEntity.created(URI.create("/api/manutencoes/" + criada.id())).body(criada);
    }

    @PostMapping("/{id}/conclusao")
    @PreAuthorize("hasRole('TECNICO')")
    public ManutencaoDto concluir(@PathVariable Long id,
                                  @Valid @RequestBody ConcluirManutencaoRequest req) {
        return service.concluir(id, req.custoReparo());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO','GERENTE')")
    public List<ManutencaoDto> historico(@RequestParam String numeroSerie) {
        return service.historico(numeroSerie);
    }

    // SUJEITO A ALTERAÇÕES FORTÍSSIMAS: ver como o esqueleto guarda o id do usuário no token
    
    /*  idDoUsuario: se o JWT tem o id como subject, o código funciona (ou deve)!
        
        O id do técnico sempre vem do token, nunca do corpo (RNF005 - Qualquer coisa conferir depois) */
    
    private Long idDoUsuario(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}