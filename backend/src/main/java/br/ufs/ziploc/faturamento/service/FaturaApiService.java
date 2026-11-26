package br.ufs.ziploc.faturamento.service;


import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.ufs.ziploc.faturamento.api.FaturaApi;
import br.ufs.ziploc.faturamento.api.FaturaResumo;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service 
@RequiredArgsConstructor 

public class FaturaApiService implements FaturaApi{

    private final FaturaService faturaService;

    @Transactional
    @Override 

    public FaturaResumo gerarFatura(Long contratoId,Long locadoraId, Long usuarioId, BigDecimal valorMulta){
        return converterParaResumo(faturaService.gerarFatura(contratoId, locadoraId, usuarioId, valorMulta));
    }

    private static FaturaResumo converterParaResumo(FaturaDto faturaDto){
        return new FaturaResumo(
            faturaDto.id(),
            faturaDto.contratoId(),
            faturaDto.valorLocacao(),
            faturaDto.valorMulta(),
            faturaDto.dataVencimento()
        );
    }
}
