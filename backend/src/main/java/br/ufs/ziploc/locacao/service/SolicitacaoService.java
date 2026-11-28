package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.cliente.api.ClienteApi;
import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.disponibilidade.api.DisponibilidadeApi;
import br.ufs.ziploc.disponibilidade.api.ItemPedidoDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.equipamento.api.ModeloInfo;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.ItemSolicitacao;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import br.ufs.ziploc.locacao.dto.ItemContratoDTO;
import br.ufs.ziploc.locacao.dto.ItemSolicitacaoDTO;
import br.ufs.ziploc.locacao.dto.RegistrarSolicitacaoRequest;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoResponse;
import br.ufs.ziploc.locacao.dto.SolicitacaoResumo;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.shared.persistence.ViolacaoIntegridade;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private static final ZoneId FUSO_LOCAL = ZoneId.of("America/Sao_Paulo");

    private final SolicitacaoRepository solicitacaoRepository;
    private final ContratoRepository contratoRepository;
    private final DisponibilidadeApi disponibilidadeApi;
    private final UnidadeApi unidadeApi;
    private final LocadoraApi locadoraApi;
    private final ClienteApi clienteApi;

    @Transactional
    public SolicitacaoResponse registrar(RegistrarSolicitacaoRequest r, Long registradoPorId) {
        if (r.dataInicioDesejada().isBefore(LocalDate.now(FUSO_LOCAL))) {
            throw new IllegalArgumentException("A data de início desejada não pode estar no passado.");
        }

        Long locadoraId = obterLocadoraId();

        exigirClienteApto(r.clienteId(), locadoraId);

        Solicitacao solicitacao = new Solicitacao(locadoraId, r.clienteId(), registradoPorId, r.dataInicioDesejada(), r.dataFimDesejada());

        Set<Long> modelosVistos = new HashSet<>();
        for (RegistrarSolicitacaoRequest.Item item : r.itens()) {
            if (!modelosVistos.add(item.modeloId())) {
                throw new IllegalArgumentException("O modelo " + item.modeloId() + " aparece mais de uma vez; some as quantidades em um único item.");
            }
            ModeloInfo modelo = unidadeApi.buscarModelo(item.modeloId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo de equipamento " + item.modeloId() + " não encontrado."));
            if (!modelo.ativo()) {
                throw new IllegalStateException("O modelo '" + modelo.nome() + "' está inativo e não pode ser solicitado.");
            }
            solicitacao.adicionarItem(item.modeloId(), item.quantidade(), modelo.valorDiariaBase());
        }
        return SolicitacaoResponse.de(salvar(solicitacao));
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResumo> listar(StatusSolicitacao status) {
        Long locadoraId = obterLocadoraId();

        List<Solicitacao> solicitacoes = status == null
                ? solicitacaoRepository.findByLocadoraIdOrderByDataCriacaoAscIdAsc(locadoraId)
                : solicitacaoRepository.findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(locadoraId, status);

        return solicitacoes.stream().map(SolicitacaoResumo::de).toList();
    }

    /** RD011: somente clientes ativos e adimplentes da locadora podem registrar solicitações. */
    private void exigirClienteApto(Long clienteId, Long locadoraId) {
        ClienteResumoDTO cliente = clienteApi.consultarResumo(clienteId);

        if (!Objects.equals(locadoraId, cliente.locadoraId())) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado com id: " + clienteId);
        }

        if (!cliente.ativo()) {
            throw new IllegalStateException("O cliente está inativo e não pode registrar solicitações.");
        }

        if (cliente.statusAdimplencia() != StatusAdimplencia.ADIMPLENTE) {
            throw new IllegalStateException("O cliente está inadimplente: o registro de novas solicitações está bloqueado (RD011).");
        }
    }

    private Long obterLocadoraId() {
        Long locadoraId = Objects.requireNonNull(
                locadoraApi.obterParametros().id(),
                "O identificador da locadora é obrigatório."
        );

        if (locadoraId <= 0) {
            throw new IllegalStateException("O identificador da locadora é inválido.");
        }

        return locadoraId;
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse buscar(Long id) {
        return SolicitacaoResponse.de(carregar(id));
    }

    /** RD012: justificativa obrigatória; só solicitações PENDENTE podem ser canceladas. */
    @Transactional
    public SolicitacaoResponse cancelar(Long id, String justificativa) {
        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação " + id + " não encontrada."));
        solicitacao.cancelar(justificativa);
        return SolicitacaoResponse.de(solicitacao);
    }

    @Transactional
    public ContratoDTO aprovar(Long id, Long gerenteId, BigDecimal percentualMulta, String condicoesDevolucao) {
        if (condicoesDevolucao == null || condicoesDevolucao.isBlank()) {
            throw new IllegalArgumentException("As condições de devolução são obrigatórias.");
        }

        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));

        solicitacao.aprovar(gerenteId);

        List<ItemPedidoDTO> pedido = solicitacao.getItens().stream()
                .map(i -> new ItemPedidoDTO(i.getModeloId(), i.getQuantidade()))
                .toList();

        List<UnidadeDTO> unidades = disponibilidadeApi.alocarUnidades(pedido, solicitacao.getDataInicioDesejada(), solicitacao.getDataFimDesejada());

        // Preserva as diárias registradas na solicitação, independentemente
        // de alterações posteriores nos preços do catálogo.
        Map<Long, BigDecimal> diariasRegistradas = solicitacao.getItens().stream()
                .collect(Collectors.toMap(
                        ItemSolicitacao::getModeloId,
                        ItemSolicitacao::getValorUnitario
                ));

        BigDecimal valorTotal = calcularValorTotal(solicitacao, unidades, diariasRegistradas);

        solicitacaoRepository.flush();

        Contrato contrato = new Contrato(solicitacao, valorTotal, percentualMulta, condicoesDevolucao);

        unidades.forEach(u -> contrato.adicionarItem(
                u.unidadeId(),
                u.modeloId(),
                obterDiariaRegistrada(diariasRegistradas, u.modeloId())
        ));

        return paraDTO(contratoRepository.saveAndFlush(contrato));
    }

    @Transactional
    public SolicitacaoDTO reprovar(Long id, Long gerenteId, String justificativa) {
        Solicitacao solicitacao = solicitacaoRepository.findComTravaById(id)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));

        solicitacao.rejeitar(gerenteId, justificativa);
        return paraDTO(solicitacao);
    }

    private Solicitacao carregar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação " + id + " não encontrada."));
    }

    private Solicitacao salvar(Solicitacao solicitacao) {
        try {
            return solicitacaoRepository.saveAndFlush(solicitacao);
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.CHAVE_ESTRANGEIRA)) {
                throw new IllegalArgumentException("Cliente ou usuário informado não existe.");
            }
            throw e;
        }
    }

    private static BigDecimal obterDiariaRegistrada(Map<Long, BigDecimal> diariasRegistradas, Long modeloId) {
        BigDecimal diaria = diariasRegistradas.get(modeloId);
        if (diaria == null) {
            throw new IllegalStateException("O modelo " + modeloId + " não possui diária registrada na solicitação.");
        }
        return diaria;
    }

    private static BigDecimal calcularValorTotal(Solicitacao s, List<UnidadeDTO> unidades, Map<Long, BigDecimal> diariasRegistradas) {
        return unidades.stream()
                .map(u -> obterDiariaRegistrada(diariasRegistradas, u.modeloId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(s.getQuantidadeDias()));
    }

    private static ContratoDTO paraDTO(Contrato c) {
        return new ContratoDTO(c.getId(), c.getSolicitacao().getId(), c.getClienteId(), c.getDataCriacao(), c.getDataInicioPrevista(), c.getDataFimPrevista(), c.getStatus().name(), c.getValorTotal(), c.getPercentualMulta(), c.getItens().stream().map(i -> new ItemContratoDTO(i.getId(), i.getUnidadeId(), i.getValorDiariaContratada())).toList());
    }

    private static SolicitacaoDTO paraDTO(Solicitacao s) {
        return new SolicitacaoDTO(s.getId(), s.getClienteId(), s.getRegistradoPorId(), s.getAnalisadoPorId(), s.getDataCriacao(), s.getDataAnalise(), s.getDataInicioDesejada(), s.getDataFimDesejada(), s.getStatus().name(), s.getJustificativa(), s.getItens().stream().map(i -> new ItemSolicitacaoDTO(i.getModeloId(), i.getQuantidade())).toList());
    }
}