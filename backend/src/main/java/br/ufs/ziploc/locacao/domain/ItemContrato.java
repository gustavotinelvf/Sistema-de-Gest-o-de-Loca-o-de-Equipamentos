package br.ufs.ziploc.locacao.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    @Column(name = "valor_diaria_contratada", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDiariaContratada;

    ItemContrato(Contrato contrato, Long unidadeId, BigDecimal valorDiariaContratada) {
        if (unidadeId == null) throw new IllegalArgumentException("A unidade de equipamento é obrigatória.");
        if (valorDiariaContratada == null || valorDiariaContratada.signum() <= 0) throw new IllegalArgumentException("A diária contratada deve ser maior que zero.");
        this.contrato = contrato;
        this.unidadeId = unidadeId;
        this.valorDiariaContratada = valorDiariaContratada;
        this.dataInicioPrevista = contrato.getDataInicioPrevista();
        this.dataFimPrevista = contrato.getDataFimPrevista();
    }

    public void registrarInicioReal(LocalDate data) {
        this.dataInicioReal = data;
    }

    public boolean isDevolvido() {
        return dataDevolucaoReal != null;
    }

    void registrarDevolucao(LocalDate data) {
        if (data == null) throw new IllegalArgumentException("A data de devolução é obrigatória.");
        if (isDevolvido()) throw new IllegalStateException("A unidade deste item já foi devolvida.");
        if (dataInicioReal == null) throw new IllegalStateException("A unidade deste item ainda não foi retirada.");
        if (data.isBefore(dataInicioReal)) throw new IllegalArgumentException("A devolução não pode ser anterior à retirada.");
        this.dataDevolucaoReal = data;
    }
}