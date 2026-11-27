package br.ufs.ziploc.faturamento.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Generated;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fatura")
@Builder

public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;

    @Column(name = "contrato_id", unique = true, nullable = false)
    private Long contratoId;

    @Column(name = "valor_locacao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLocacao;

    @Column(name = "valor_multa", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMulta;

    @Column(name = "valor_total", insertable = false, updatable = false, precision = 13, scale = 2)
    @Generated
    private BigDecimal valorTotal;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private StatusFatura status;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "responsavel_emissao_id", nullable = false)
    private Long responsavelEmissaoId;

    @Column(name = "responsavel_pagamento_id")
    private Long responsavelPagamentoId;

    public void marcarPago(LocalDate dataPagamento, Long usuarioId) {

        if(status != StatusFatura.PENDENTE && status != StatusFatura.VENCIDA){
            throw new IllegalStateException(
                "Fatura com status " + status + " não pode ser paga.");
        }

        if(dataPagamento == null || usuarioId == null){
            throw new IllegalArgumentException(
                "Data de pagamento e usuário responsável são obrigatórios.");
        }

        if(dataEmissao == null || dataPagamento.isBefore(dataEmissao)){
            throw new IllegalArgumentException(
                "A data de pagamento não pode ser anterior à data de emissão.");
        }

        this.dataPagamento = dataPagamento;
        this.responsavelPagamentoId = usuarioId;
        this.status = StatusFatura.PAGA;

    }

}