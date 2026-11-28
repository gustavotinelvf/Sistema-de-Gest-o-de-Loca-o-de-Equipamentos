package br.ufs.ziploc.locacao.dto;

import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.StatusContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha da listagem de contratos; os itens ficam no detalhe (GET /api/contratos/{id}). */
public record ContratoListagemResponse(Long id, Long clienteId, Long solicitacaoId, LocalDate dataInicioPrevista,
                                       LocalDate dataFimPrevista, StatusContrato status, BigDecimal valorTotal) {

    public static ContratoListagemResponse de(Contrato c) {
        return new ContratoListagemResponse(c.getId(), c.getClienteId(),
                c.getSolicitacao() == null ? null : c.getSolicitacao().getId(),
                c.getDataInicioPrevista(), c.getDataFimPrevista(), c.getStatus(), c.getValorTotal());
    }
}
