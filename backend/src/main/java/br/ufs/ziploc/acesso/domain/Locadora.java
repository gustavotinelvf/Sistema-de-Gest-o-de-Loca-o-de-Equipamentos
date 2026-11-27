package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Configuração da locadora. No schema do Michael o id é gerado (identity) e outras
 * tabelas apontam para ela por locadora_id; o sistema usa uma só (a primeira).
 */
@Entity
@Table(name = "locadora")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Embedded
    private EnderecoLocadora endereco;

    @Column(name = "raio_entrega_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal raioEntregaKm;

    // No banco a coluna se chama percentual_multa_atraso; no código seguimos o nome do DTO.
    @Column(name = "percentual_multa_atraso", precision = 5, scale = 2)
    private BigDecimal percentualMultaPadrao;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    public Locadora(String razaoSocial, String cnpj, EnderecoLocadora endereco,
                    BigDecimal raioEntregaKm, BigDecimal percentualMultaPadrao) {
        if (raioEntregaKm == null || raioEntregaKm.signum() <= 0) {
            throw new IllegalArgumentException("O raio de entrega deve ser maior que zero");
        }
        if (percentualMultaPadrao == null || percentualMultaPadrao.signum() < 0
                || percentualMultaPadrao.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("O percentual de multa deve estar entre 0 e 100");
        }
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.raioEntregaKm = raioEntregaKm;
        this.percentualMultaPadrao = percentualMultaPadrao;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCadastro == null) {
            dataCadastro = Instant.now();
        }
    }
}
