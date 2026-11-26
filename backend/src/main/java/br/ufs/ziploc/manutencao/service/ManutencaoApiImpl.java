package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.manutencao.api.ManutencaoResumo;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ManutencaoApiImpl implements ManutencaoApi {

    private final ManutencaoRepository manutencoes;

    @Override
    @Transactional
    public ManutencaoResumo abrirAPartirDeVistoria(Long locadoraId, Long unidadeId,
                                                   Long tecnicoId, String descricaoAvarias) {
        Manutencao manutencao = manutencoes.findFirstByUnidadeIdAndDataConclusaoIsNull(unidadeId)
                .orElseGet(() -> manutencoes.save(new Manutencao(locadoraId, unidadeId, tecnicoId,
                        TipoManutencao.CORRETIVA, descricaoAvarias, null, Instant.now())));

        return new ManutencaoResumo(manutencao.getId(), manutencao.getUnidadeId(),
                manutencao.getTipo().name(), manutencao.getDataInicio());
    }
}