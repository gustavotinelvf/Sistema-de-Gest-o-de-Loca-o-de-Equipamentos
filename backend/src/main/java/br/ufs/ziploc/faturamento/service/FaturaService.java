package br.ufs.ziploc.faturamento.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import br.ufs.ziploc.faturamento.domain.Fatura;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.exception.FaturaNaoEncontradaException;
import br.ufs.ziploc.faturamento.provisorio.ContratoApi;
import br.ufs.ziploc.faturamento.repository.FaturaRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 

public class FaturaService {

    private static final ZoneId FUSO_LOCAL = ZoneId.of("America/Sao_Paulo");
    private final FaturaRepository faturaRepository;
    private final ContratoApi contratoApi;

    @Transactional

    public FaturaDto registrarPagamento(Long faturaId,Long locadoraId, Long usuarioId){
        
        Fatura fatura = faturaRepository.findByIdAndLocadoraId(faturaId, locadoraId)
        .orElseThrow(()-> new FaturaNaoEncontradaException(faturaId));

        LocalDate dataPagamento = LocalDate.now(FUSO_LOCAL);

        fatura.marcarPago(dataPagamento, usuarioId);

        contratoApi.finalizarContrato(fatura.getContratoId());

        return FaturaDto.recebimentoFatura(fatura);
    }
}
