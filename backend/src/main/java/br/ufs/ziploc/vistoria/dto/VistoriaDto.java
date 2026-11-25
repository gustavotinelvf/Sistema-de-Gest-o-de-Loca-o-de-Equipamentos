package br.ufs.ziploc.vistoria.dto;

import br.ufs.ziploc.vistoria.domain.StatusVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;

import java.math.BigDecimal;
import java.time.Instant;

public record VistoriaDto(
        Long id,
        Long devolucaoId,
        Long usuarioTecnicoId,
        Instant dataVistoria,
        StatusVistoria status,
        BigDecimal custoDano,
        String laudo) {

    public static VistoriaDto de(Vistoria v) {
        return new VistoriaDto(v.getId(), v.getDevolucaoId(), v.getUsuarioTecnicoId(), v.getDataVistoria(),
                v.getStatus(), v.getCustoDano(), v.getLaudo());
    }
}