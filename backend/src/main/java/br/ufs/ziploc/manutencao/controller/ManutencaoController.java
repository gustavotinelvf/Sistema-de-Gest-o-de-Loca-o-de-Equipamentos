package br.ufs.ziploc.manutencao.controller;

import br.ufs.ziploc.manutencao.dto.ConcluirManutencaoRequest;
import br.ufs.ziploc.manutencao.dto.IniciarManutencaoRequest;
import br.ufs.ziploc.manutencao.dto.ManutencaoDto;
import br.ufs.ziploc.manutencao.service.ManutencaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    @PostMapping
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<ManutencaoDto> iniciar(@Valid @RequestBody IniciarManutencaoRequest request,
                                                 Authentication autenticacao) {
        /* TODO: locadoraId provisório, igual ao DevolucaoController, até a segurança fornecer a locadora do usuário. */
        Long locadoraId = 1L;

        ManutencaoDto criada = manutencaoService.iniciarAvulsa(locadoraId, request.numeroSerie(), request.tipo(),
                request.descricao(), request.dataPrevista(), Long.valueOf(autenticacao.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PostMapping("/{id}/conclusao")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<ManutencaoDto> concluir(@PathVariable("id") Long id,
                                                  @Valid @RequestBody ConcluirManutencaoRequest request) {
        return ResponseEntity.ok(manutencaoService.concluir(id, request.custoGerado(), request.justificativaAtraso()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ResponseEntity<List<ManutencaoDto>> historico(@RequestParam("numeroSerie") String numeroSerie) {
        return ResponseEntity.ok(manutencaoService.historico(numeroSerie));
    }
}