package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.ItemContrato;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import br.ufs.ziploc.locacao.dto.ContratoResponse;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.ItemContratoRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.shared.persistence.ViolacaoIntegridade;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContratoService implements ContratoApi {

    private static final ZoneId FUSO_LOCAL = ZoneId.of("America/Sao_Paulo");

    private final ContratoRepository contratos;
    private final ItemContratoRepository itemContratoRepository;
    private final UnidadeApi unidadeApi;
    private final EntityManager entityManager;

    /**
     * Retirada: PLANEJADO → ATIVO, registra o início real de cada item e marca as unidades como LOCADA.
     * Tudo ou nada: qualquer unidade indisponível desfaz a transação inteira.
     */
    @Transactional
    public ContratoResponse retirar(Long id, LocalDate dataRetirada) {
        Contrato contrato = carregar(id);
        if (contrato.getStatus() != StatusContrato.PLANEJADO) {
            throw new IllegalStateException("Só é possível retirar contratos planejados (status atual: " + contrato.getStatus() + ").");
        }
        if (contrato.getItens().isEmpty()) {
            throw new IllegalStateException("O contrato não possui unidades para retirada.");
        }
        List<UnidadeInfo> unidades = new ArrayList<>();
        for (ItemContrato item : contrato.getItens()) {
            UnidadeInfo unidade = buscarUnidade(item.getUnidadeId());
            if (!unidade.disponivel()) {
                throw new IllegalStateException("A unidade " + unidade.numeroSerie() + " não está disponível para retirada (status "
                        + unidade.status() + (unidade.ativo() ? "" : ", inativa") + ").");
            }
            unidades.add(unidade);
        }

        LocalDate dia = dataRetirada != null ? dataRetirada : LocalDate.now(FUSO_LOCAL);
        contrato.ativar();
        for (ItemContrato item : contrato.getItens()) {
            item.registrarInicioReal(dia);
        }
        unidades.forEach(u -> unidadeApi.locar(u.id()));
        return ContratoResponse.de(contratos.saveAndFlush(contrato));
    }

    /**
     * Cancelamento (RD012: justificativa obrigatória). Só contratos PLANEJADO (máquina de estados) e,
     * ao cancelar, os itens são removidos para liberar as unidades da constraint anti-overbooking.
     */
    @Transactional
    public ContratoResponse cancelar(Long id, String justificativa) {
        Contrato contrato = carregar(id);
        contrato.cancelar(justificativa);
        contrato.liberarReservas();
        return ContratoResponse.de(contratos.saveAndFlush(contrato));
    }

    /**
     * Prorrogação de contrato ATIVO. Acréscimo = soma das diárias contratadas das unidades × dias adicionados.
     * A disponibilidade no período novo é garantida pela constraint do banco (409 se houver sobreposição).
     */
    @Transactional
    public ContratoResponse prorrogar(Long id, LocalDate novaDataFim) {
        Contrato contrato = carregar(id);
        if (contrato.getStatus() != StatusContrato.ATIVO) {
            throw new IllegalStateException("Só é possível prorrogar contratos ativos (status atual: " + contrato.getStatus() + ").");
        }
        if (novaDataFim == null || !novaDataFim.isAfter(contrato.getDataFimPrevista())) {
            throw new IllegalArgumentException("A nova data de fim deve ser posterior à data de fim atual (" + contrato.getDataFimPrevista() + ").");
        }
        // O banco estende o período de todos os itens ao mudar o contrato; uma unidade já devolvida ficaria bloqueada sem necessidade.
        if (contrato.getItens().stream().anyMatch(i -> i.getDataDevolucaoReal() != null)) {
            throw new IllegalStateException("Não é possível prorrogar um contrato que já teve unidades devolvidas.");
        }

        long diasAdicionais = ChronoUnit.DAYS.between(contrato.getDataFimPrevista(), novaDataFim);
        BigDecimal diariasPorDia = BigDecimal.ZERO;
        for (ItemContrato item : contrato.getItens()) {
            diariasPorDia = diariasPorDia.add(item.getValorDiariaContratada());
        }
        BigDecimal acrescimo = diariasPorDia.multiply(BigDecimal.valueOf(diasAdicionais)).setScale(2, RoundingMode.HALF_UP);

        contrato.prorrogar(novaDataFim, acrescimo);
        try {
            contratos.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.EXCLUSAO)) {
                throw new ConflitoException("Uma ou mais unidades já estão reservadas em outro contrato até " + novaDataFim + ".");
            }
            throw e;
        }
        // As datas dos itens foram propagadas pelo banco (ON UPDATE CASCADE); recarrega para refletir na resposta.
        entityManager.refresh(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional(readOnly = true)
    public ContratoResponse buscar(Long id) {
        return ContratoResponse.de(carregar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContratoResumo> buscarContrato(Long contratoId) {
        return contratos.findById(contratoId).map(ContratoService::paraResumo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemContratoPrazo> buscarItemContrato(Long itemContratoId) {
        return itemContratoRepository.findById(itemContratoId).map(ContratoService::paraResumo);
    }

    @Override
    @Transactional
    public void registrarDevolucao(Long itemContratoId, LocalDate dataDevolucao) {
        ItemContrato item = itemContratoRepository.findById(itemContratoId).orElseThrow(() -> new ContratoNaoEncontradoException("Item de contrato " + itemContratoId + " não encontrado."));
        item.getContrato().registrarDevolucao(itemContratoId, dataDevolucao);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarItensPendentes(Long contratoId) {
        if (!contratos.existsById(contratoId)) throw naoEncontrado(contratoId);
        return itemContratoRepository.countByContratoIdAndDataDevolucaoRealIsNull(contratoId);
    }

    @Override
    @Transactional
    public void finalizarContrato(Long contratoId) {
        contratos.findById(contratoId).orElseThrow(() -> naoEncontrado(contratoId)).finalizar();
    }

    private Contrato carregar(Long id) {
        return contratos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato " + id + " não encontrado."));
    }

    private UnidadeInfo buscarUnidade(Long unidadeId) {
        return unidadeApi.buscarUnidade(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade de equipamento " + unidadeId + " não encontrada."));
    }

    private static ContratoNaoEncontradoException naoEncontrado(Long contratoId) {
        return new ContratoNaoEncontradoException("Contrato " + contratoId + " não encontrado.");
    }

    private static ContratoResumo paraResumo(Contrato c) {
        return new ContratoResumo(c.getId(), c.getClienteId(), c.getStatus().name(), c.getDataInicioPrevista(), c.getDataFimPrevista(), c.getValorTotal(), c.getPercentualMulta(), c.getItens().stream().map(ContratoService::paraResumo).toList());
    }

    private static ItemContratoPrazo paraResumo(ItemContrato i) {
        Contrato c = i.getContrato();
        return new ItemContratoPrazo(i.getId(), c.getId(), c.getClienteId(), i.getUnidadeId(), i.getDataInicioPrevista(), i.getDataFimPrevista(), i.getDataInicioReal(), i.getDataDevolucaoReal(), i.getValorDiariaContratada(), c.getPercentualMulta(), c.getValorTotal());
    }
}