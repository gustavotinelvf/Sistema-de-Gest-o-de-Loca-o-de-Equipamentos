package br.ufs.ziploc.equipamento.controller;

import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.dto.AlterarNumeroSerieRequest;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoRequest;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoResponse;
import br.ufs.ziploc.equipamento.service.UnidadeEquipamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-equipamento")
@RequiredArgsConstructor
public class UnidadeEquipamentoController {

    private final UnidadeEquipamentoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public ResponseEntity<UnidadeEquipamentoResponse> criar(@Valid @RequestBody UnidadeEquipamentoRequest request) {
        UnidadeEquipamentoResponse criada = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(criada.id())).body(criada);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'ATENDENTE', 'GERENTE', 'TECNICO')")
    public List<UnidadeEquipamentoResponse> listar(@RequestParam(required = false) Long modeloId,
                                                   @RequestParam(required = false) String categoria,
                                                   @RequestParam(required = false) StatusUnidade status,
                                                   @RequestParam(required = false) Boolean ativo) {
        return service.listar(modeloId, categoria, status, ativo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'ATENDENTE', 'GERENTE', 'TECNICO')")
    public UnidadeEquipamentoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public UnidadeEquipamentoResponse alterarNumeroSerie(@PathVariable Long id, @Valid @RequestBody AlterarNumeroSerieRequest request) {
        return service.alterarNumeroSerie(id, request.numeroSerie());
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public UnidadeEquipamentoResponse inativar(@PathVariable Long id) {
        return service.inativar(id);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public UnidadeEquipamentoResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}