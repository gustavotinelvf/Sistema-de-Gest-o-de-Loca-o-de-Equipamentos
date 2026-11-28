package br.ufs.ziploc.vistoria.dto;

import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;

import java.time.Instant;

public record VistoriaDto(
        Long id,
        Long devolucaoId,
        Long tecnicoId,
        Instant dataInicio,
        Instant dataConclusao,
        ResultadoVistoria resultado,
        String descricaoAvarias) {

    public static VistoriaDto de(Vistoria v) {
        return new VistoriaDto(v.getId(), v.getDevolucaoId(), v.getTecnicoId(), v.getDataInicio(),
                v.getDataConclusao(), v.getResultado(), v.getDescricaoAvarias());
    }
}