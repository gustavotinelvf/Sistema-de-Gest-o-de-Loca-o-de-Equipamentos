package br.ufs.ziploc.manutencao.dto;

import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;

import java.math.BigDecimal;
import java.time.Instant;

public record ManutencaoDto(
        Long id,
        Long unidadeId,
        Long tecnicoId,
        TipoManutencao tipo,
        String descricao,
        BigDecimal custoGerado,
        Instant dataInicio,
        Instant dataPrevista,
        Instant dataConclusao,
        String justificativaAtraso) {

    public static ManutencaoDto de(Manutencao m) {
        return new ManutencaoDto(m.getId(), m.getUnidadeId(), m.getTecnicoId(), m.getTipo(), m.getDescricao(),
                m.getCustoGerado(), m.getDataInicio(), m.getDataPrevista(), m.getDataConclusao(),
                m.getJustificativaAtraso());
    }
}