package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "item_contrato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @Column(name = "solicitacao_id", nullable = false)
    private Long solicitacaoId;
    @Column(name = "item_solicitacao_id", nullable = false)
    private Long itemSolicitacaoId;
    @Column(name = "modelo_id", nullable = false)
    private Long modeloId;
    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;
    @Column(name = "data_inicio_prevista", nullable = false, updatable = false)
    private LocalDate dataInicioPrevista;
    @Column(name = "data_fim_prevista", nullable = false, updatable = false)
    private LocalDate dataFimPrevista;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_contrato", nullable = false, insertable = false, updatable = false)
    private StatusContrato statusContrato;
    @Column(name = "valor_diaria", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDiariaContratada;
    @Formula("(SELECT CAST(d.data_devolucao_real AT TIME ZONE 'America/Sao_Paulo' AS date) FROM devolucao d WHERE d.item_contrato_id = id)")
    private LocalDate dataDevolucaoReal;

    ItemContrato(Contrato contrato, ItemSolicitacao itemSolicitacao, Long unidadeId, BigDecimal valorDiariaContratada) {
        if (contrato == null) throw new IllegalArgumentException("O contrato é obrigatório.");
        if (itemSolicitacao == null) throw new IllegalArgumentException("O item da solicitação é obrigatório.");
        if (itemSolicitacao.getSolicitacao() != contrato.getSolicitacao()) throw new IllegalArgumentException("O item não pertence à solicitação do contrato.");
        if (unidadeId == null) throw new IllegalArgumentException("A unidade de equipamento é obrigatória.");
        if (valorDiariaContratada == null || valorDiariaContratada.signum() <= 0) throw new IllegalArgumentException("A diária contratada deve ser maior que zero.");
        this.contrato = contrato;
        this.locadoraId = contrato.getLocadoraId();
        this.solicitacaoId = contrato.getSolicitacao().getId();
        this.itemSolicitacaoId = itemSolicitacao.getId();
        this.modeloId = itemSolicitacao.getModeloId();
        this.unidadeId = unidadeId;
        this.valorDiariaContratada = valorDiariaContratada;
        this.dataInicioPrevista = contrato.getDataInicioPrevista();
        this.dataFimPrevista = contrato.getDataFimPrevista();
    }

    public LocalDate getDataInicioReal() {
        return contrato.getDataRetirada() == null ? null :
                contrato.getDataRetirada().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();
    }

    public boolean isDevolvido() {
        return dataDevolucaoReal != null;
    }
}