package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "modelo_id", nullable = false)
    private Long modeloId;
    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    ItemSolicitacao(Solicitacao solicitacao, Long modeloId, int quantidade) {
        if (modeloId == null) throw new IllegalArgumentException("O modelo de equipamento é obrigatório.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        this.solicitacao = solicitacao;
        this.modeloId = modeloId;
        this.quantidade = quantidade;
    }
}
