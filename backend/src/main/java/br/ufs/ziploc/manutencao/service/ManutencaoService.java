package br.ufs.ziploc.manutencao.service;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
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

    /** Manutenção avulsa: o técnico informa o número de série (sem vistoria). */
    @Transactional
    public ManutencaoDto iniciarAvulsa(String numeroSerie, String descricaoProblema, Long usuarioTecnicoId) {
        Long unidadeId = buscarUnidadeId(numeroSerie);

        // Lança IllegalStateException (409) se a unidade não puder entrar em manutenção.
        unidadeApi.iniciarManutencao(unidadeId);

        Manutencao manutencao = new Manutencao(unidadeId, null, usuarioTecnicoId, descricaoProblema, Instant.now());
        return ManutencaoDto.de(manutencoes.saveAndFlush(manutencao));
    }

    @Transactional
    public ManutencaoDto concluir(Long id, BigDecimal custoReparo) {
        Manutencao manutencao = manutencoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Manutenção " + id + " não encontrada."));

        manutencao.concluir(Instant.now(), custoReparo.setScale(2, RoundingMode.HALF_UP));
        unidadeApi.liberar(manutencao.getUnidadeId());
        Manutencao salva = manutencoes.saveAndFlush(manutencao);

        /*
         * Só manutenção originada de vistoria gera dano para a fatura. A avulsa (vistoriaId nulo) não tem
         * devolução por trás. A Devolução resolve unidade -> item do contrato -> contrato do lado dela.
         * TODO: PROVISÓRIO até existir DevolucaoApi.registrarDanos(Long unidadeId, BigDecimal valor) na devolucao/api.
         */
        if (salva.getVistoriaId() != null) {
            devolucaoApi.registrarDanos(salva.getUnidadeId(), salva.getCustoReparo());
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

    private Long buscarUnidadeId(String numeroSerie) {
        return unidadeApi.buscarUnidadeIdPorNumeroSerie(numeroSerie)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade com número de série " + numeroSerie + " não encontrada."));
    }
}