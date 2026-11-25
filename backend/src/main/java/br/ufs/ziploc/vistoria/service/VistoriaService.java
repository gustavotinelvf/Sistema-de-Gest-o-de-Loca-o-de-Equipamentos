package br.ufs.ziploc.vistoria.service;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.shared.persistence.ViolacaoIntegridade;
import br.ufs.ziploc.vistoria.domain.StatusVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.dto.VistoriaDto;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VistoriaService {

    private static final String UNIDADE_EM_VISTORIA = "EM_VISTORIA";

    private final VistoriaRepository vistorias;
    private final DevolucaoApi devolucaoApi;
    private final ContratoApi contratoApi;
    private final UnidadeApi unidadeApi;
    private final ManutencaoApi manutencaoApi;

    @Transactional
    public VistoriaDto registrar(Long devolucaoId, StatusVistoria status, BigDecimal custoDano,
                                 String laudo, Long usuarioTecnicoId) {
        DevolucaoResumo devolucao = devolucaoApi.buscarPorId(devolucaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Devolução " + devolucaoId + " não encontrada."));
        ItemContratoPrazo item = contratoApi.buscarItemContrato(devolucao.itemContratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Item de contrato " + devolucao.itemContratoId() + " não encontrado."));

        // As regras de dados (RD022 e custo) ficam na entidade e lançam IllegalArgumentException (422).
        Vistoria vistoria = new Vistoria(devolucaoId, usuarioTecnicoId, Instant.now(), status, custoDano, laudo);

        if (vistorias.existsByDevolucaoId(devolucaoId)) {
            throw new ConflitoException("Já existe vistoria para a devolução " + devolucaoId + ".");
        }

        UnidadeInfo unidade = unidadeApi.buscarUnidade(item.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade de equipamento " + item.unidadeId() + " não encontrada."));
        if (!UNIDADE_EM_VISTORIA.equals(unidade.status())) {
            throw new IllegalStateException("A unidade " + unidade.numeroSerie()
                    + " não está em vistoria (status atual: " + unidade.status() + ").");
        }

        Vistoria salva;
        try {
            salva = vistorias.saveAndFlush(vistoria);
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.UNICA)) {
                throw new ConflitoException("Já existe vistoria para a devolução " + devolucaoId + ".");
            }
            throw e;
        }

        if (salva.reprovada()) {
            unidadeApi.iniciarManutencao(item.unidadeId());
            manutencaoApi.abrirAPartirDeVistoria(salva.getId(), item.unidadeId(), usuarioTecnicoId, salva.getLaudo());
        } else {
            unidadeApi.liberar(item.unidadeId());
        }

        /*
         * TODO (quando o Faturamento tiver a FaturaApi): atualizar os danos da fatura do contrato, com
         * faturaApi.atualizarDanos(item.contratoId(), vistoriaApi.totalizarDanosDoContrato(item.contratoId()));
         * O saveAndFlush acima garante que a vistoria recém-salva entra na soma.
         */

        return VistoriaDto.de(salva);
    }
}