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
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.dto.VistoriaDto;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public VistoriaDto registrar(Long locadoraId, Long devolucaoId, ResultadoVistoria resultado,
                                 String descricaoAvarias, Long tecnicoId) {
        DevolucaoResumo devolucao = devolucaoApi.buscarPorId(devolucaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Devolução " + devolucaoId + " não encontrada."));
        ItemContratoPrazo item = contratoApi.buscarItemContrato(devolucao.itemContratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Item de contrato " + devolucao.itemContratoId() + " não encontrado."));

        Instant agora = Instant.now();

        // Se já existe uma vistoria iniciada para a devolução, conclui ela; se não, cria e conclui.
        Vistoria vistoria = vistorias.findByDevolucaoId(devolucaoId)
                .orElseGet(() -> new Vistoria(locadoraId, devolucaoId, tecnicoId, agora));

        // IllegalState (409) se já concluída; IllegalArgument (422) para RD022.
        vistoria.concluir(resultado, descricaoAvarias, agora);

        // O trigger devolucao_registrar já colocou a unidade em EM_VISTORIA; aqui só validamos (409 claro).
        UnidadeInfo unidade = unidadeApi.buscarUnidade(item.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade de equipamento " + item.unidadeId() + " não encontrada."));
        if (!UNIDADE_EM_VISTORIA.equals(unidade.status())) {
            throw new IllegalStateException("A unidade " + unidade.numeroSerie()
                    + " não está em vistoria (status atual: " + unidade.status() + ").");
        }

        // O trigger vistoria_registrar muda a unidade ao salvar: APROVADA -> DISPONIVEL, REPROVADA -> EM_MANUTENCAO.
        Vistoria salva;
        try {
            salva = vistorias.saveAndFlush(vistoria);
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.UNICA)) {
                throw new ConflitoException("Já existe vistoria para a devolução " + devolucaoId + ".");
            }
            throw e;
        }

        // O trigger não cria a manutenção; a linha corretiva é aberta aqui, depois da vistoria salva.
        if (salva.reprovada()) {
            manutencaoApi.abrirAPartirDeVistoria(locadoraId, item.unidadeId(), tecnicoId, salva.getDescricaoAvarias());
        }

        return VistoriaDto.de(salva);
    }
}