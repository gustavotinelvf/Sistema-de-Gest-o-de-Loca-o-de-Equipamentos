package br.ufs.ziploc.devolucao.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.exceptions.ItemContratoNaoEncontradoException;
import br.ufs.ziploc.devolucao.provisorio.ContratoApi;
import br.ufs.ziploc.devolucao.provisorio.ItemContratoPrazo;
import br.ufs.ziploc.devolucao.provisorio.StatusUnidade;
import br.ufs.ziploc.devolucao.provisorio.UnidadeApi;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 

public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final ContratoApi contratoApi;
    private final UnidadeApi unidadeApi;

    @Transactional 
    public DevolucaoDto registrar(RegistrarDevolucaoDto request, Long locadoraId, Long usuarioId ){
        ItemContratoPrazo itemContrato = contratoApi.buscarItemContrato(request.itemContratoId()).orElseThrow(
            () ->new ItemContratoNaoEncontradoException(request.itemContratoId())
        );
        
         /*trocar por Clock quando houver testes da multa */
        Instant dataDevolucao = Instant.now();

        Devolucao devolucao = Devolucao.builder()
        .locadoraId(locadoraId)
        .itemContratoId(request.itemContratoId())
        .usuarioId(usuarioId)
        .dataDevolucaoReal(dataDevolucao)
        .multaAtraso(BigDecimal.ZERO)
        .observacoes(request.observacoes())
        .build();

        devolucaoRepository.save(devolucao);

        contratoApi.registrarDevolucao(request.itemContratoId(), dataDevolucao);

        unidadeApi.alterarStatus(itemContrato.unidadeId(), StatusUnidade.EM_VISTORIA);

        return DevolucaoDto.recebimentoDevolucao(devolucao);


    }

}
