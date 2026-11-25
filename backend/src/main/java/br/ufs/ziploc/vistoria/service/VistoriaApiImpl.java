package br.ufs.ziploc.vistoria.service;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.api.VistoriaApi;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class VistoriaApiImpl implements VistoriaApi {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final VistoriaRepository vistorias;
    private final DevolucaoApi devolucaoApi;
    private final ContratoApi contratoApi;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalizarDanosDoItem(Long itemContratoId) {
        return devolucaoApi.buscarPorItemContratoId(itemContratoId)
                .flatMap(devolucao -> vistorias.findByDevolucaoId(devolucao.id()))
                .map(Vistoria::getCustoDano)
                .orElse(ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalizarDanosDoContrato(Long contratoId) {
        ContratoResumo contrato = contratoApi.buscarContrato(contratoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato " + contratoId + " não encontrado."));

        return contrato.itens().stream()
                .map(item -> totalizarDanosDoItem(item.id()))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}