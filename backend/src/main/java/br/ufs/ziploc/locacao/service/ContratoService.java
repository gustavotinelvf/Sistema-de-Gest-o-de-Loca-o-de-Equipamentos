package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.ItemContrato;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.ItemContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContratoService implements ContratoApi {
    private final ContratoRepository contratoRepository;
    private final ItemContratoRepository itemContratoRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ContratoResumo> buscarContrato(Long contratoId) {
        return contratoRepository.findById(contratoId).map(ContratoService::paraResumo);
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
        if (!contratoRepository.existsById(contratoId)) throw naoEncontrado(contratoId);
        return itemContratoRepository.countByContratoIdAndDataDevolucaoRealIsNull(contratoId);
    }

    @Override
    @Transactional
    public void finalizarContrato(Long contratoId) {
        contratoRepository.findById(contratoId).orElseThrow(() -> naoEncontrado(contratoId)).finalizar();
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
