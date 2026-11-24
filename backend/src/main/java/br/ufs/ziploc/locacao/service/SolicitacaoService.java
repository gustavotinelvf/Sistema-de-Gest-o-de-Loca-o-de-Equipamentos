package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeApi;
import br.ufs.ziploc.disponibilidade.api.ItemPedidoDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import br.ufs.ziploc.locacao.dto.ItemContratoDTO;
import br.ufs.ziploc.locacao.dto.ItemSolicitacaoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {
    private final SolicitacaoRepository solicitacaoRepository;
    private final ContratoRepository contratoRepository;
    private final DisponibilidadeApi disponibilidadeApi;

    @Transactional
    public ContratoDTO aprovar(Long id, Long gerenteId, BigDecimal percentualMulta) {
        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id).orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
        solicitacao.aprovar(gerenteId);
        List<ItemPedidoDTO> pedido = solicitacao.getItens().stream().map(i -> new ItemPedidoDTO(i.getModeloId(), i.getQuantidade())).toList();
        List<UnidadeDTO> unidades = disponibilidadeApi.alocarUnidades(pedido, solicitacao.getDataInicioDesejada(), solicitacao.getDataFimDesejada());
        Contrato contrato = new Contrato(solicitacao, calcularValorTotal(solicitacao, unidades), percentualMulta);
        unidades.forEach(u -> contrato.adicionarItem(u.unidadeId(), u.valorDiaria()));
        return paraDTO(contratoRepository.saveAndFlush(contrato));
    }

    @Transactional
    public SolicitacaoDTO reprovar(Long id, Long gerenteId, String justificativa) {
        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id).orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
        solicitacao.rejeitar(gerenteId, justificativa);
        return paraDTO(solicitacao);
    }

    private static BigDecimal calcularValorTotal(Solicitacao s, List<UnidadeDTO> unidades) {
        long dias = ChronoUnit.DAYS.between(s.getDataInicioDesejada(), s.getDataFimDesejada()) + 1;
        return unidades.stream().map(UnidadeDTO::valorDiaria).reduce(BigDecimal.ZERO, BigDecimal::add).multiply(BigDecimal.valueOf(dias));
    }

    private static ContratoDTO paraDTO(Contrato c) {
        return new ContratoDTO(c.getId(), c.getSolicitacao().getId(), c.getClienteId(), c.getDataCriacao(), c.getDataInicioPrevista(), c.getDataFimPrevista(), c.getStatus().name(), c.getValorTotal(), c.getPercentualMulta(), c.getItens().stream().map(i -> new ItemContratoDTO(i.getId(), i.getUnidadeId(), i.getValorDiariaContratada())).toList());
    }

    private static SolicitacaoDTO paraDTO(Solicitacao s) {
        return new SolicitacaoDTO(s.getId(), s.getClienteId(), s.getRegistradoPorId(), s.getAnalisadoPorId(), s.getDataCriacao(), s.getDataAnalise(), s.getDataInicioDesejada(), s.getDataFimDesejada(), s.getStatus().name(), s.getJustificativa(), s.getItens().stream().map(i -> new ItemSolicitacaoDTO(i.getModeloId(), i.getQuantidade())).toList());
    }
}
