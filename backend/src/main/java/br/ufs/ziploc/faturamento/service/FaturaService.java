package br.ufs.ziploc.faturamento.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import br.ufs.ziploc.faturamento.domain.Fatura;
import br.ufs.ziploc.faturamento.domain.StatusFatura;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.exception.FaturaJaExisteException;
import br.ufs.ziploc.faturamento.exception.FaturaNaoEncontradaException;
import br.ufs.ziploc.faturamento.repository.FaturaRepository;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.api.ContratoResumo;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 

public class FaturaService {

    /*Regra de negócio associado ao prazo ser a data de vencimento + 7 corridos */
    private static final int PRAZO_VENCIMENTO_DIAS = 7;

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

    @Transactional
    public FaturaDto gerarFatura(Long contratoId,Long locadoraId, Long usuarioId, BigDecimal valorMulta){
        
        if(faturaRepository.findByContratoId(contratoId).isPresent()){
            throw new FaturaJaExisteException(contratoId);
        }

        ContratoResumo contrato = contratoApi.buscarContrato(contratoId).orElseThrow(
            () -> new ContratoNaoEncontradoException("O contrato " +contratoId+ " não foi encontrado")
        );

        LocalDate dataEmissao = LocalDate.now(FUSO_LOCAL);

        LocalDate dataVencimento = contrato.dataFimPrevista().plusDays(PRAZO_VENCIMENTO_DIAS);

        Fatura fatura = Fatura.builder()
        .contratoId(contratoId)
        .locadoraId(locadoraId)
        .valorLocacao(contrato.valorTotal())
        .valorMulta(valorMulta) /*O módulo de Devolução garante que o valorMulta não seja passado como nulo */
        .status(StatusFatura.PENDENTE)
        .dataEmissao(dataEmissao)
        .dataVencimento(dataVencimento)
        .responsavelEmissaoId(usuarioId)
        .build();

        fatura = faturaRepository.save(fatura);

        return FaturaDto.recebimentoFatura(fatura);

    }
}
