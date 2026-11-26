package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.acesso.repository.LocadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa a LocadoraApi. Só leitura: a locadora é criada pela carga inicial. */
@Service
@RequiredArgsConstructor
public class LocadoraService implements LocadoraApi {

    private final LocadoraRepository locadoraRepository;

    @Override
    @Transactional(readOnly = true)
    public ParametrosLocadoraDTO obterParametros() {
        return locadoraRepository.findUnica()
                .map(ParametrosLocadoraDTO::from)
                .orElseThrow(() -> new IllegalStateException(
                        "Locadora não configurada: a carga inicial ainda não foi executada"));
    }
}
