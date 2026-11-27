package br.ufs.ziploc.devolucao.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 

public class DevolucaoApiService implements DevolucaoApi{

    private final DevolucaoRepository devolucaoRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<DevolucaoResumo> buscarPorId(Long id){
        return devolucaoRepository.findById(id).map(DevolucaoApiService::converterParaResumo);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<DevolucaoResumo> buscarPorItemContratoId(Long itemContratoId){
        return devolucaoRepository.findByItemContratoId(itemContratoId).map(DevolucaoApiService::converterParaResumo);
    }

    private static DevolucaoResumo converterParaResumo(Devolucao devolucao){
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
