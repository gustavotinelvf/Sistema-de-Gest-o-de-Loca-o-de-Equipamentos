package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.dto.ItemSolicitacaoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {
    private final SolicitacaoRepository solicitacaoRepository;

    @Transactional
    public SolicitacaoDTO reprovar(Long id, Long gerenteId, String justificativa) {
        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id).orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
        solicitacao.rejeitar(gerenteId, justificativa);
        return paraDTO(solicitacao);
    }

    private static SolicitacaoDTO paraDTO(Solicitacao s) {
        return new SolicitacaoDTO(s.getId(), s.getClienteId(), s.getRegistradoPorId(), s.getAnalisadoPorId(), s.getDataCriacao(), s.getDataAnalise(), s.getDataInicioDesejada(), s.getDataFimDesejada(), s.getStatus().name(), s.getJustificativa(), s.getItens().stream().map(i -> new ItemSolicitacaoDTO(i.getModeloId(), i.getQuantidade())).toList());
    }
}
