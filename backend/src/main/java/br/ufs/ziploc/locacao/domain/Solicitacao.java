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
@Table(name = "solicitacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @Column(name = "solicitante_id", nullable = false)
    private Long registradoPorId;
    @Column(name = "aprovador_id")
    private Long analisadoPorId;
    @Column(name = "data_solicitacao", nullable = false, updatable = false)
    private Instant dataCriacao;
    @Column(name = "data_analise")
    private Instant dataAnalise;
    @Column(name = "data_inicio_prevista", nullable = false)
    private LocalDate dataInicioDesejada;
    @Column(name = "data_fim_prevista", nullable = false)
    private LocalDate dataFimDesejada;
    @Column(name = "distancia_entrega_km", precision = 8, scale = 2)
    private BigDecimal distanciaKm;
    @Column(name = "valor_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorEstimado = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusSolicitacao status;
    @Column(name = "justificativa")
    private String justificativa;
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemSolicitacao> itens = new ArrayList<>();

    public Solicitacao(Long locadoraId, Long clienteId, Long registradoPorId, LocalDate dataInicioDesejada, LocalDate dataFimDesejada) {
        if (locadoraId == null || locadoraId <= 0) throw new IllegalArgumentException("A locadora é obrigatória.");
        if (clienteId == null) throw new IllegalArgumentException("O cliente é obrigatório.");
        if (registradoPorId == null) throw new IllegalArgumentException("O usuário solicitante é obrigatório.");
        if (dataInicioDesejada == null || dataFimDesejada == null || !dataFimDesejada.isAfter(dataInicioDesejada)) throw new IllegalArgumentException("A data de fim deve ser posterior à data de início.");
        this.locadoraId = locadoraId;
        this.clienteId = clienteId;
        this.registradoPorId = registradoPorId;
        this.dataInicioDesejada = dataInicioDesejada;
        this.dataFimDesejada = dataFimDesejada;
        this.status = StatusSolicitacao.PENDENTE;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCriacao == null) dataCriacao = Instant.now();
    }

    public List<ItemSolicitacao> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void adicionarItem(Long modeloId, int quantidade, BigDecimal valorUnitario) {
        exigirPendente();
        itens.add(new ItemSolicitacao(this, modeloId, quantidade, valorUnitario));
        this.valorEstimado = valorEstimado.add(valorUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public boolean isDistanciaVerificada() {
        return distanciaKm != null;
    }

    public void registrarDistancia(BigDecimal distanciaKm) {
        exigirPendente();
        if (distanciaKm == null || distanciaKm.signum() < 0) throw new IllegalArgumentException("A distância não pode ser nula ou negativa.");
        this.distanciaKm = distanciaKm;
    }

    public void aprovar(Long analisadoPorId) {
        exigirPendente();
        registrarAnalise(analisadoPorId);
        this.status = StatusSolicitacao.APROVADA;
    }

    public void rejeitar(Long analisadoPorId, String justificativa) {
        exigirPendente();
        this.justificativa = exigirJustificativa(justificativa);
        registrarAnalise(analisadoPorId);
        this.status = StatusSolicitacao.REJEITADA;
    }

    public void cancelar(String justificativa) {
        exigirPendente();
        this.justificativa = exigirJustificativa(justificativa);
        this.status = StatusSolicitacao.CANCELADA;
    }

    private void registrarAnalise(Long analisadoPorId) {
        if (analisadoPorId == null) throw new IllegalArgumentException("O usuário responsável pela análise é obrigatório.");
        this.analisadoPorId = analisadoPorId;
        this.dataAnalise = Instant.now();
    }

    private void exigirPendente() {
        if (status != StatusSolicitacao.PENDENTE) throw new IllegalStateException("Apenas solicitações pendentes podem ser alteradas (status atual: " + status + ").");
    }

    private static String exigirJustificativa(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("A justificativa é obrigatória.");
        return justificativa.strip();
    }
}