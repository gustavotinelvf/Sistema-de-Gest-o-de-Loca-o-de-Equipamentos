package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "contrato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private Solicitacao solicitacao;
    @Column(name = "data_formalizacao", nullable = false, updatable = false)
    private Instant dataCriacao;
    @Column(name = "data_inicio_prevista", nullable = false)
    private LocalDate dataInicioPrevista;
    @Column(name = "data_fim_prevista", nullable = false)
    private LocalDate dataFimPrevista;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusContrato status;
    @Column(name = "valor_locacao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;
    @Column(name = "valor_entrega", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorEntrega = BigDecimal.ZERO;
    @Column(name = "distancia_entrega_km", precision = 8, scale = 2)
    private BigDecimal distanciaEntregaKm;
    @Column(name = "percentual_multa_atraso", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualMulta;
    @Column(name = "condicoes_devolucao", nullable = false)
    private String condicoesDevolucao;
    @Column(name = "justificativa_cancelamento")
    private String justificativaCancelamento;
    @Column(name = "data_retirada")
    private Instant dataRetirada;
    @Column(name = "responsavel_retirada_id")
    private Long responsavelRetiradaId;
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemContrato> itens = new ArrayList<>();

    public Contrato(Solicitacao solicitacao, BigDecimal valorTotal, BigDecimal percentualMulta, String condicoesDevolucao) {
        if (solicitacao == null || solicitacao.getStatus() != StatusSolicitacao.APROVADA) throw new IllegalStateException("Apenas solicitações aprovadas podem gerar contrato.");
        if (valorTotal == null || valorTotal.signum() <= 0) throw new IllegalArgumentException("O valor da locação deve ser maior que zero.");
        if (percentualMulta == null || percentualMulta.signum() < 0 || percentualMulta.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("O percentual de multa deve estar entre 0 e 100.");
        if (condicoesDevolucao == null || condicoesDevolucao.isBlank()) throw new IllegalArgumentException("As condições de devolução são obrigatórias.");
        if (solicitacao.getAnalisadoPorId() == null) throw new IllegalArgumentException("O responsável pelo contrato é obrigatório.");
        this.solicitacao = solicitacao;
        this.locadoraId = solicitacao.getLocadoraId();
        this.clienteId = solicitacao.getClienteId();
        this.responsavelId = solicitacao.getAnalisadoPorId();
        this.dataInicioPrevista = solicitacao.getDataInicioDesejada();
        this.dataFimPrevista = solicitacao.getDataFimDesejada();
        this.distanciaEntregaKm = solicitacao.getDistanciaKm();
        this.valorTotal = valorTotal;
        this.percentualMulta = percentualMulta;
        this.condicoesDevolucao = condicoesDevolucao.strip();
        this.status = StatusContrato.PLANEJADO;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCriacao == null) dataCriacao = Instant.now();
    }

    public List<ItemContrato> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void adicionarItem(Long unidadeId, Long modeloId, BigDecimal valorDiariaContratada) {
        if (status != StatusContrato.PLANEJADO) throw new IllegalStateException("Só é possível adicionar itens a contratos planejados.");

        ItemSolicitacao itemSolicitacao = solicitacao.getItens().stream()
                .filter(i -> i.getModeloId().equals(modeloId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("O modelo " + modeloId + " não pertence à solicitação."));

        if (itens.stream().anyMatch(i -> i.getUnidadeId().equals(unidadeId))) throw new IllegalArgumentException("A unidade já pertence ao contrato.");

        long quantidadeAlocada = itens.stream().filter(i -> i.getModeloId().equals(modeloId)).count();
        if (quantidadeAlocada >= itemSolicitacao.getQuantidade()) throw new IllegalStateException("A quantidade alocada excede a quantidade solicitada.");

        itens.add(new ItemContrato(this, itemSolicitacao, unidadeId, valorDiariaContratada));
    }

    public long contarItensPendentes() {
        return itens.stream().filter(i -> !i.isDevolvido()).count();
    }

    public void ativar(Instant dataRetirada, Long responsavelRetiradaId) {
        if (dataRetirada == null) throw new IllegalArgumentException("A data de retirada é obrigatória.");
        if (responsavelRetiradaId == null || responsavelRetiradaId <= 0) throw new IllegalArgumentException("O responsável pela retirada é obrigatório.");
        transicionarPara(StatusContrato.ATIVO);
        this.dataRetirada = dataRetirada;
        this.responsavelRetiradaId = responsavelRetiradaId;
    }

    public void finalizar() {
        if (contarItensPendentes() > 0) throw new IllegalStateException("O contrato só pode ser finalizado após a devolução de todas as unidades.");
        transicionarPara(StatusContrato.FINALIZADO);
    }

    public void cancelar(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("A justificativa de cancelamento é obrigatória.");
        transicionarPara(StatusContrato.CANCELADO);
        this.justificativaCancelamento = justificativa.strip();
    }

    /**
     * Estende o fim do contrato. O banco propaga a nova data aos itens (FK composta com ON UPDATE CASCADE)
     * e a constraint anti-overbooking rejeita a mudança se alguma unidade já estiver reservada no período novo.
     */
    public void prorrogar(LocalDate novaDataFim, BigDecimal acrescimoValor) {
        if (status != StatusContrato.ATIVO) throw new IllegalStateException("Só é possível prorrogar contratos ativos (status atual: " + status + ").");
        if (novaDataFim == null || !novaDataFim.isAfter(dataFimPrevista)) throw new IllegalArgumentException("A nova data de fim deve ser posterior à data de fim atual (" + dataFimPrevista + ").");
        if (acrescimoValor == null || acrescimoValor.signum() < 0) throw new IllegalArgumentException("O acréscimo de valor não pode ser negativo.");
        this.dataFimPrevista = novaDataFim;
        this.valorTotal = valorTotal.add(acrescimoValor);
    }

    private void transicionarPara(StatusContrato destino) {
        if (!status.podeTransicionarPara(destino)) throw new IllegalStateException("Transição de status inválida: " + status + " → " + destino + ".");
        this.status = destino;
    }
}