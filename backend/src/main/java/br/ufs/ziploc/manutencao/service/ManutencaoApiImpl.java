package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.manutencao.api.ManutencaoResumo;
import br.ufs.ziploc.manutencao.domain.Manutencao;
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
    public ManutencaoResumo abrirAPartirDeVistoria(Long vistoriaId, Long unidadeId,
                                                   Long usuarioTecnicoId, String descricaoProblema) {
        Manutencao manutencao = manutencoes.findByVistoriaId(vistoriaId)
                .orElseGet(() -> manutencoes.save(
                        new Manutencao(unidadeId, vistoriaId, usuarioTecnicoId, descricaoProblema, Instant.now())));

        return new ManutencaoResumo(manutencao.getId(), manutencao.getUnidadeId(), manutencao.getVistoriaId(),
                manutencao.getStatus().name(), manutencao.getDataInicio());
    }
}