package br.ufs.ziploc.locacao.dto;

import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitacaoResponse(Long id, Long clienteId, Long registradoPorId, Instant dataCriacao,
                                  LocalDate dataInicioDesejada, LocalDate dataFimDesejada,
                                  BigDecimal valorEstimado, StatusSolicitacao status, String justificativa, List<Item> itens) {

    public record Item(Long modeloId, int quantidade) {
    }

    public static SolicitacaoResponse de(Solicitacao s) {
        return new SolicitacaoResponse(s.getId(), s.getClienteId(), s.getRegistradoPorId(), s.getDataCriacao(),
                s.getDataInicioDesejada(), s.getDataFimDesejada(), s.getValorEstimado(), s.getStatus(), s.getJustificativa(),
                s.getItens().stream().map(i -> new Item(i.getModeloId(), i.getQuantidade())).toList());
    }
}
