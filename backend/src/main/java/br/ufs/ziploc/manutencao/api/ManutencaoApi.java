package br.ufs.ziploc.manutencao.api;

import br.ufs.ziploc.manutencao.dto.ManutencaoDto;

public interface ManutencaoApi {
    ManutencaoDto abrirAPartirDeVistoria(Long vistoriaId, Long unidadeId,
                                         Long tecnicoId, String descricaoProblema);
}