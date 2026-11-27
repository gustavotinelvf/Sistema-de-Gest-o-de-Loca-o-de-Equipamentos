package br.ufs.ziploc.cliente.controller;

import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.ClienteRequestDTO;
import br.ufs.ziploc.cliente.dto.ClienteResponseDTO;
import br.ufs.ziploc.cliente.service.ClienteService;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id", "razaoSocial", "cnpj", "ativo",
            "statusAdimplencia", "dataCadastro"
    );

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<ClienteResponseDTO> cadastrar(@Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO criado = clienteService.cadastrar(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(criado.id()))
                .body(criado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE', 'FINANCEIRO')")
    public Page<ClienteResponseDTO> listar(
            @RequestParam(required = false) String razaoSocial,
            @RequestParam(required = false) String cnpj,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) StatusAdimplencia statusAdimplencia,
            @PageableDefault(size = 20, sort = "razaoSocial") Pageable pageable
    ) {
        return clienteService.listar(
                razaoSocial, cnpj, ativo, statusAdimplencia, normalizarPaginacao(pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE', 'FINANCEIRO')")
    public ClienteResponseDTO buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE', 'FINANCEIRO')")
    public ClienteResponseDTO buscarPorCnpj(@PathVariable String cnpj) {
        return clienteService.buscarPorCnpj(cnpj);
    }

    @GetMapping("/cnpj")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE', 'FINANCEIRO')")
    public ClienteResponseDTO buscarPorCnpjComParametro(@RequestParam String cnpj) {
        return clienteService.buscarPorCnpj(cnpj);
    }

    private Pageable normalizarPaginacao(Pageable pageable) {
        Sort ordenacao = pageable.getSort();

        for (Sort.Order ordem : ordenacao) {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new IllegalArgumentException(
                        "Campo de ordenação inválido: " + ordem.getProperty()
                );
            }
        }

        if (ordenacao.isUnsorted()) {
            ordenacao = Sort.by("razaoSocial");
        }

        if (ordenacao.getOrderFor("id") == null) {
            ordenacao = ordenacao.and(Sort.by("id"));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                ordenacao
        );
    }
}