package br.ufs.ziploc.devolucao.service;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DevolucaoApiService implements DevolucaoApi {

    private final DevolucaoRepository devolucaoRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<DevolucaoResumo> buscarPorId(Long id) {
        return devolucaoRepository.findById(id)
                .map(DevolucaoApiService::converterParaResumo);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<DevolucaoResumo> buscarPorItemContratoId(Long itemContratoId) {
        return devolucaoRepository.findByItemContratoId(itemContratoId)
                .map(DevolucaoApiService::converterParaResumo);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<DevolucaoResumo> buscarPorIdParaAtualizacao(Long id, Long locadoraId) {
        return devolucaoRepository.findByIdAndLocadoraIdForUpdate(id, locadoraId)
                .map(DevolucaoApiService::converterParaResumo);
    }

    private static DevolucaoResumo converterParaResumo(Devolucao devolucao) {
        return new DevolucaoResumo(
                devolucao.getId(),
                devolucao.getItemContratoId(),
                devolucao.getUsuarioId(),
                devolucao.getDataDevolucaoReal(),
                devolucao.getMultaAtraso(),
                devolucao.getObservacoes()
        );
    }
}