package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.dto.ManutencaoDto;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManutencaoService {

    /**
     * Status em que o banco recusa uma nova manutenção (trigger manutencao_registrar e
     * índice manutencao_aberta_unica). Aqui só validamos para devolver 409 com mensagem clara.
     */
    private static final Set<String> STATUS_QUE_IMPEDEM_MANUTENCAO =
            Set.of("LOCADA", "EM_VISTORIA", "EM_MANUTENCAO");

    private final ManutencaoRepository manutencoes;
    private final UnidadeApi unidadeApi;

    /** Manutenção avulsa: o técnico informa o número de série. */
    @Transactional
    public ManutencaoDto iniciarAvulsa(Long locadoraId, String numeroSerie, TipoManutencao tipo,
                                       String descricao, Instant dataPrevista, Long tecnicoId) {
        Long unidadeId = buscarUnidadeId(numeroSerie);

        String status = unidadeApi.buscarUnidade(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade de equipamento " + unidadeId + " não encontrada."))
                .status();
        if (STATUS_QUE_IMPEDEM_MANUTENCAO.contains(status)) {
            throw new IllegalStateException(
                    "A unidade está " + status + " e não pode entrar em manutenção.");
        }

        // O trigger do banco muda a unidade para EM_MANUTENCAO ao inserir a manutenção.
        Manutencao manutencao = new Manutencao(locadoraId, unidadeId, tecnicoId, tipo, descricao,
                dataPrevista, Instant.now());
        return ManutencaoDto.de(manutencoes.saveAndFlush(manutencao));
    }

    @Transactional
    public ManutencaoDto concluir(Long id, BigDecimal custoGerado, String justificativaAtraso) {
        Manutencao manutencao = manutencoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Manutenção " + id + " não encontrada."));

        // IllegalState (409) se já concluída; IllegalArgument (422) para RD023 e dados inválidos.
        manutencao.concluir(Instant.now(), custoGerado.setScale(2, RoundingMode.HALF_UP), justificativaAtraso);

        // O trigger do banco devolve a unidade a DISPONIVEL ao salvar a conclusão.
        return ManutencaoDto.de(manutencoes.saveAndFlush(manutencao));
    }

    @Transactional(readOnly = true)
    public List<ManutencaoDto> historico(String numeroSerie) {
        Long unidadeId = buscarUnidadeId(numeroSerie);
        return manutencoes.findByUnidadeIdOrderByDataInicioDesc(unidadeId).stream()
                .map(ManutencaoDto::de)
                .toList();
    }

    private Long buscarUnidadeId(String numeroSerie) {
        return unidadeApi.buscarUnidadeIdPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade com número de série " + numeroSerie + " não encontrada."));
    }
}