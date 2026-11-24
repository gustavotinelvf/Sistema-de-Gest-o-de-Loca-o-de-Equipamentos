package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Configuração global da locadora: existe uma única linha (id = 1).
 * O id NÃO é gerado: o banco tem CHECK (id = 1) e DEFAULT 1.
 */
@Entity
@Table(name = "locadora")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locadora {

    public static final long ID_UNICO = 1L;

    @Id
    @Column(name = "id")
    private Long id = ID_UNICO;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Embedded
    private EnderecoLocadora endereco;

    @Column(name = "raio_entrega_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal raioEntregaKm;

    @Column(name = "percentual_multa_padrao", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualMultaPadrao;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    public Locadora(String razaoSocial, String cnpj, EnderecoLocadora endereco,
                    BigDecimal raioEntregaKm, BigDecimal percentualMultaPadrao) {
        if (raioEntregaKm == null || raioEntregaKm.signum() <= 0) {
            throw new IllegalArgumentException("O raio de entrega deve ser maior que zero");
        }
        if (percentualMultaPadrao == null || percentualMultaPadrao.signum() < 0) {
            throw new IllegalArgumentException("O percentual de multa não pode ser negativo");
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
