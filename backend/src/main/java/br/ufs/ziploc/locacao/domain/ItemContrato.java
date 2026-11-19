package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;
    @Column(name = "data_inicio_prevista", nullable = false)
    private LocalDate dataInicioPrevista;
    @Column(name = "data_fim_prevista", nullable = false)
    private LocalDate dataFimPrevista;
    @Column(name = "data_inicio_real")
    private LocalDate dataInicioReal;
    @Column(name = "data_devolucao_real")
    private LocalDate dataDevolucaoReal;

    ItemContrato(Contrato contrato, Long unidadeId) {
        if (unidadeId == null) throw new IllegalArgumentException("A unidade de equipamento é obrigatória.");
        this.contrato = contrato;
        this.unidadeId = unidadeId;
        this.dataInicioPrevista = contrato.getDataInicioPrevista();
        this.dataFimPrevista = contrato.getDataFimPrevista();
    }

    public void registrarInicioReal(LocalDate data) {
        this.dataInicioReal = data;
    }
}
