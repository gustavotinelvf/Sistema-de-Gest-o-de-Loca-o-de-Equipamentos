package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "item_solicitacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSolicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @Column(name = "modelo_id", nullable = false)
    private Long modeloId;
    @Column(name = "quantidade", nullable = false)
    private int quantidade;
    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    ItemSolicitacao(Solicitacao solicitacao, Long modeloId, int quantidade, BigDecimal valorUnitario) {
        if (solicitacao == null) throw new IllegalArgumentException("A solicitação é obrigatória.");
        if (solicitacao.getLocadoraId() == null) throw new IllegalArgumentException("A locadora é obrigatória.");
        if (modeloId == null) throw new IllegalArgumentException("O modelo de equipamento é obrigatório.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        if (valorUnitario == null || valorUnitario.signum() <= 0) throw new IllegalArgumentException("O valor unitário deve ser maior que zero.");
        this.solicitacao = solicitacao;
        this.locadoraId = solicitacao.getLocadoraId();
        this.modeloId = modeloId;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }
}