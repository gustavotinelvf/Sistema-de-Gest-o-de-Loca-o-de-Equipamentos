package br.ufs.ziploc.disponibilidade.controller;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeDTO;
import br.ufs.ziploc.disponibilidade.service.DisponibilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disponibilidade")
@RequiredArgsConstructor
public class DisponibilidadeController {
    private final DisponibilidadeService disponibilidadeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<List<DisponibilidadeDTO>> consultarDisponibilidade(@RequestParam LocalDate inicio, @RequestParam LocalDate fim, @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(disponibilidadeService.consultarDisponibilidade(inicio, fim, categoria));
    }
}
