package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.dto.ManutencaoDto;
import br.ufs.ziploc.manutencao.provisorio.DevolucaoApi;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManutencaoService {

    private final ManutencaoRepository manutencoes;
    private final UnidadeApi unidadeApi;
    /* TODO: PROVISÓRIO. Trocar por br.ufs.ziploc.devolucao.api.DevolucaoApi quando o colega publicar registrarDanos. */
    private final DevolucaoApi devolucaoApi;

    /** Manutenção avulsa: o técnico informa o número de série. */
    @Transactional
    public ManutencaoDto iniciarAvulsa(Long locadoraId, String numeroSerie, TipoManutencao tipo,
                                       String descricao, Instant dataPrevista, Long tecnicoId) {
        Long unidadeId = buscarUnidadeId(numeroSerie);

        // Lança IllegalStateException (409) se a unidade não puder entrar em manutenção.
        unidadeApi.iniciarManutencao(unidadeId);

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
        unidadeApi.liberar(manutencao.getUnidadeId());
        Manutencao salva = manutencoes.saveAndFlush(manutencao);

        if (deveRepassarDanos(salva)) {
            devolucaoApi.registrarDanos(salva.getUnidadeId(), salva.getCustoGerado());
        }
        return ManutencaoDto.de(salva);
    }

    @Transactional(readOnly = true)
    public List<ManutencaoDto> historico(String numeroSerie) {
        Long unidadeId = buscarUnidadeId(numeroSerie);
        return manutencoes.findByUnidadeIdOrderByDataInicioDesc(unidadeId).stream()
                .map(ManutencaoDto::de)
                .toList();
    }

    /*
     * TODO: PROVISÓRIO. O banco (pgerd) não tem vistoria_id na manutencao, então não dá para saber se ela
     * veio de uma vistoria. Critério temporário: CORRETIVA com custo maior que zero. Problema: uma manutenção
     * avulsa CORRETIVA (unidade quebrada no estoque) também repassaria. Combinar com o grupo: coluna
     * vistoria_id em manutencao, ou outra regra. Só este método precisa mudar.
     */
    private boolean deveRepassarDanos(Manutencao manutencao) {
        return manutencao.getTipo() == TipoManutencao.CORRETIVA && manutencao.getCustoGerado().signum() > 0;
    }

    private Long buscarUnidadeId(String numeroSerie) {
        return unidadeApi.buscarUnidadeIdPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade com número de série " + numeroSerie + " não encontrada."));
    }
}