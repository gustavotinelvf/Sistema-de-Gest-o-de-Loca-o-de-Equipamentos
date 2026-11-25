package br.ufs.ziploc.equipamento.controller;

import br.ufs.ziploc.equipamento.dto.ModeloEquipamentoRequest;
import br.ufs.ziploc.equipamento.dto.ModeloEquipamentoResponse;
import br.ufs.ziploc.equipamento.service.ModeloEquipamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/modelos-equipamento")
@RequiredArgsConstructor
public class ModeloEquipamentoController {

    private final ModeloEquipamentoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public ResponseEntity<ModeloEquipamentoResponse> criar(@Valid @RequestBody ModeloEquipamentoRequest request) {
        ModeloEquipamentoResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(criado.id())).body(criado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'ATENDENTE', 'GERENTE', 'TECNICO')")
    public List<ModeloEquipamentoResponse> listar(@RequestParam(required = false) String categoria,
                                                  @RequestParam(required = false) Boolean ativo) {
        return service.listar(categoria, ativo);
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'ATENDENTE', 'GERENTE', 'TECNICO')")
    public List<String> categorias() {
        return service.listarCategorias();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'ATENDENTE', 'GERENTE', 'TECNICO')")
    public ModeloEquipamentoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public ModeloEquipamentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ModeloEquipamentoRequest request) {
        return service.atualizar(id, request);
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public ModeloEquipamentoResponse inativar(@PathVariable Long id) {
        return service.inativar(id);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE')")
    public ModeloEquipamentoResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}