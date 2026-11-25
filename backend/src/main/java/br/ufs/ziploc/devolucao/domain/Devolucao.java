package br.ufs.ziploc.devolucao.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter 
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "devolucao")
@Builder  

public class Devolucao {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locadora_id",nullable = false)
    private Long locadoraId;

    @Column(name ="item_contrato_id",nullable = false,unique = true)
    private Long itemContratoId;

    @Column(name = "usuario_id",nullable = false)
    private Long usuarioId;

    @Column(name = "data_devolucao_real",nullable = false)
    private Instant dataDevolucaoReal;

    @Column(name = "multa_atraso", nullable = false)
    private BigDecimal multaAtraso;

    @Column(name = "observacoes")
    private String observacoes;
}
