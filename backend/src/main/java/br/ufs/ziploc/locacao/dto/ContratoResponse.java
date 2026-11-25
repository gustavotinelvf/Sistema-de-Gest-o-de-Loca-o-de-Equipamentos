package br.ufs.ziploc.locacao.dto;

import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.StatusContrato;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContratoResponse(Long id, Long clienteId, Long solicitacaoId, Instant dataCriacao,
                               LocalDate dataInicioPrevista, LocalDate dataFimPrevista, StatusContrato status,
                               BigDecimal valorTotal, BigDecimal percentualMulta, String justificativaCancelamento,
                               List<Item> itens) {

    public record Item(Long id, Long unidadeId, LocalDate dataInicioPrevista, LocalDate dataFimPrevista,
                       LocalDate dataInicioReal, LocalDate dataDevolucaoReal) {
    }

    public static ContratoResponse de(Contrato c) {
        return new ContratoResponse(c.getId(), c.getClienteId(),
                c.getSolicitacao() == null ? null : c.getSolicitacao().getId(),
                c.getDataCriacao(), c.getDataInicioPrevista(), c.getDataFimPrevista(), c.getStatus(),
                c.getValorTotal(), c.getPercentualMulta(), c.getJustificativaCancelamento(),
                c.getItens().stream().map(i -> new Item(i.getId(), i.getUnidadeId(), i.getDataInicioPrevista(),
                        i.getDataFimPrevista(), i.getDataInicioReal(), i.getDataDevolucaoReal())).toList());
    }
}
