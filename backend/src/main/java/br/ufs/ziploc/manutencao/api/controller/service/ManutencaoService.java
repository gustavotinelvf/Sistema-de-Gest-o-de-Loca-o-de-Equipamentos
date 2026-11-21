package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.equipamento.api.EquipamentoApi;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.StatusManutencao;
import br.ufs.ziploc.manutencao.dto.ManutencaoDto;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.shared.erro.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/* Alguns pontos para entender melhor!

#   @Transactional: se alterarStatus falhar, o INSERT da manutenção é desfeito.
       É tudo ou nada

#   A entidade nunca sai do service: sempre ManutencaoDto.de(...).

#   Em concluir não há repository.save porque a entidade já é gerenciada
       e o JPA grava no final da transação
#   O setScale(2, HALF_UP) evita problemas de arredondamento (dinheiro NUNCA é double).
#   OBS = A regra “unidade precisa estar em certo estado para entrar em manutenção”
       não está definida, precisa ser confirmada com a Equipe 2 se a UnidadeApi já recusa transições inválidas. */

@Service
public class ManutencaoService implements ManutencaoApi {

    private static final String UNIDADE_EM_MANUTENCAO = "EM_MANUTENCAO";
    private static final String UNIDADE_DISPONIVEL = "DISPONIVEL";
    private static final Set<StatusManutencao> ABERTAS =
            Set.of(StatusManutencao.AGUARDANDO, StatusManutencao.EM_ANDAMENTO);

    private final ManutencaoRepository repository;
    private final UnidadeApi unidadeApi;
    private final EquipamentoApi equipamentoApi;

    public ManutencaoService(ManutencaoRepository repository,
                             UnidadeApi unidadeApi,
                             EquipamentoApi equipamentoApi) {
        this.repository = repository;
        this.unidadeApi = unidadeApi;
        this.equipamentoApi = equipamentoApi;
    }

    // Manutenção avulsa: o técnico informa o número de série!!
    @Transactional
    public ManutencaoDto iniciarAvulsa(String numeroSerie, String descricao, Long tecnicoId) {
        Long unidadeId = equipamentoApi.buscarUnidadeIdPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> ApiException.naoEncontrado("RN_UNIDADE_NAO_ENCONTRADA",
                        "Unidade com número de série " + numeroSerie + " não encontrada."));

        if (repository.existsByUnidadeIdAndStatusIn(unidadeId, ABERTAS)) {
            throw ApiException.conflito("RN_MANUTENCAO_JA_ABERTA",
                    "Esta unidade já possui uma manutenção em aberto.");
        }

        unidadeApi.alterarStatus(unidadeId, UNIDADE_EM_MANUTENCAO);
        Manutencao salva = repository.save(Manutencao.nova(unidadeId, null, tecnicoId, descricao));
        return ManutencaoDto.de(salva);
    }

    // Chamada pela Vistoria quando reprovada: A Vistoria já mudou o status da unidade
    @Override
    @Transactional
    public ManutencaoDto abrirAPartirDeVistoria(Long vistoriaId, Long unidadeId,
                                                Long tecnicoId, String descricaoProblema) {
        // idempotência: se já existe manutenção dessa vistoria, devolve a existente
        return repository.findByVistoriaId(vistoriaId)
                .map(ManutencaoDto::de)
                .orElseGet(() -> ManutencaoDto.de(repository.save(
                        Manutencao.nova(unidadeId, vistoriaId, tecnicoId, descricaoProblema))));
    }

    @Transactional
    public ManutencaoDto concluir(Long id, BigDecimal custoReparo) {
        Manutencao m = repository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("RN_MANUTENCAO_NAO_ENCONTRADA",
                        "Manutenção " + id + " não encontrada."));

        if (m.estaConcluida()) {
            throw ApiException.regraNegocio("RN_MANUTENCAO_JA_CONCLUIDA",
                    "Esta manutenção já foi concluída.");
        }

        BigDecimal custo = custoReparo.setScale(2, RoundingMode.HALF_UP);
        m.concluir(Instant.now(), custo);
        unidadeApi.alterarStatus(m.getUnidadeId(), UNIDADE_DISPONIVEL);
        return ManutencaoDto.de(m);   // JPA vai gravar no commit (a entidade já gerenciada)
    }

    @Transactional(readOnly = true)
    public List<ManutencaoDto> historico(String numeroSerie) {
        Long unidadeId = equipamentoApi.buscarUnidadeIdPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> ApiException.naoEncontrado("RN_UNIDADE_NAO_ENCONTRADA",
                        "Unidade com número de série " + numeroSerie + " não encontrada."));
        return repository.findByUnidadeIdOrderByDataInicioDesc(unidadeId)
                .stream().map(ManutencaoDto::de).toList();
    }
}