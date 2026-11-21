package br.ufs.ziploc.manutencao.dto;

import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.StatusManutencao;

import java.math.BigDecimal;
import java.time.Instant;

public record ManutencaoDto(
        Long id, Long unidadeId, Long vistoriaId, Long usuarioTecnicoId,
        Instant dataInicio, Instant dataFim, String descricaoProblema,
        BigDecimal custoReparo, StatusManutencao status) {

    public static ManutencaoDto de(Manutencao m) {
        return new ManutencaoDto(m.getId(), m.getUnidadeId(), m.getVistoriaId(),
                m.getUsuarioTecnicoId(), m.getDataInicio(), m.getDataFim(),
                m.getDescricaoProblema(), m.getCustoReparo(), m.getStatus());
    }
}