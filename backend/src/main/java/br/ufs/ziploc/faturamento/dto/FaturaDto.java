package br.ufs.ziploc.faturamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.ufs.ziploc.faturamento.domain.Fatura;
import br.ufs.ziploc.faturamento.domain.StatusFatura;

public record FaturaDto(
    Long id, 
    Long contratoId, 
    BigDecimal valorLocacao,
    BigDecimal valorMulta,
    BigDecimal valorTotal,
    StatusFatura status,
    LocalDate dataEmissao,
    LocalDate dataVencimento,
    LocalDate dataPagamento
) {

    public static FaturaDto recebimentoFatura(Fatura fatura){
        return new FaturaDto(
            fatura.getId(),
            fatura.getContratoId(), 
            fatura.getValorLocacao(), 
            fatura.getValorMulta(), 
            fatura.getValorTotal(), 
            fatura.getStatus(), 
            fatura.getDataEmissao(), 
            fatura.getDataVencimento(), 
            fatura.getDataPagamento());
    }

}
