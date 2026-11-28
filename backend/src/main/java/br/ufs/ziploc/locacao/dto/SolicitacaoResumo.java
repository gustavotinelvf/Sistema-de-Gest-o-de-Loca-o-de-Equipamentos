package br.ufs.ziploc.locacao.dto;

import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SolicitacaoResumo(Long id, Long clienteId, Long registradoPorId, Instant dataCriacao, LocalDate dataInicioDesejada, LocalDate dataFimDesejada, BigDecimal valorEstimado, StatusSolicitacao status) {

    public static SolicitacaoResumo de(Solicitacao s) {
        return new SolicitacaoResumo(s.getId(), s.getClienteId(), s.getRegistradoPorId(), s.getDataCriacao(), s.getDataInicioDesejada(), s.getDataFimDesejada(), s.getValorEstimado(), s.getStatus());
    }
}
