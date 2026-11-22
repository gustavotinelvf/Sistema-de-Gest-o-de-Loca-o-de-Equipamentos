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
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", unique = true)
    private Solicitacao solicitacao;
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;
    @Column(name = "data_inicio_prevista", nullable = false)
    private LocalDate dataInicioPrevista;
    @Column(name = "data_fim_prevista", nullable = false)
    private LocalDate dataFimPrevista;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusContrato status;
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    @Column(name = "percentual_multa", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualMulta;
    @Column(name = "justificativa_cancelamento")
    private String justificativaCancelamento;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemContrato> itens = new ArrayList<>();

    public Contrato(Solicitacao solicitacao, BigDecimal valorTotal, BigDecimal percentualMulta) {
        if (solicitacao.getStatus() != StatusSolicitacao.APROVADA) throw new IllegalStateException("Apenas solicitações aprovadas podem gerar contrato.");
        if (valorTotal.signum() < 0 || percentualMulta.signum() < 0) throw new IllegalArgumentException("Valor total e percentual de multa não podem ser negativos.");
        this.solicitacao = solicitacao;
        this.clienteId = solicitacao.getClienteId();
        this.dataInicioPrevista = solicitacao.getDataInicioDesejada();
        this.dataFimPrevista = solicitacao.getDataFimDesejada();
        this.valorTotal = valorTotal;
        this.percentualMulta = percentualMulta;
        this.status = StatusContrato.PLANEJADO;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCriacao == null) dataCriacao = Instant.now();
    }

    public List<ItemContrato> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void adicionarItem(Long unidadeId, BigDecimal valorDiariaContratada) {
        if (status != StatusContrato.PLANEJADO) throw new IllegalStateException("Só é possível adicionar itens a contratos planejados.");
        itens.add(new ItemContrato(this, unidadeId, valorDiariaContratada));
    }

    public void registrarDevolucao(Long itemContratoId, LocalDate data) {
        if (status != StatusContrato.ATIVO) throw new IllegalStateException("Só é possível registrar devolução em contratos ativos.");
        itens.stream().filter(i -> itemContratoId.equals(i.getId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Item " + itemContratoId + " não pertence ao contrato " + id + ".")).registrarDevolucao(data);
    }

    public long contarItensPendentes() {
        return itens.stream().filter(i -> !i.isDevolvido()).count();
    }

    public void ativar() {
        transicionarPara(StatusContrato.ATIVO);
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

    private void transicionarPara(StatusContrato destino) {
        if (!status.podeTransicionarPara(destino)) throw new IllegalStateException("Transição de status inválida: " + status + " → " + destino + ".");
        this.status = destino;
    }
}
