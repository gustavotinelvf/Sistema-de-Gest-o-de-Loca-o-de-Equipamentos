package br.ufs.ziploc.equipamento.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "modelo_equipamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloEquipamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @Column(name = "nome", nullable = false, length = 200)
    private String nome;
    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;
    @Column(name = "especificacoes", nullable = false)
    private String descricao;
    @Column(name = "valor_diaria", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDiariaBase;
    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima;
    @Column(name = "quantidade_maxima")
    private Integer quantidadeMaxima;
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    public ModeloEquipamento(Long locadoraId, String nome, String categoria, String descricao, BigDecimal valorDiariaBase, int quantidadeMinima, Integer quantidadeMaxima) {
        if (locadoraId == null || locadoraId <= 0) throw new IllegalArgumentException("A locadora é obrigatória.");
        this.locadoraId = locadoraId;
        atualizar(nome, categoria, descricao, valorDiariaBase, quantidadeMinima, quantidadeMaxima);
        this.ativo = true;
    }

    public void atualizar(String nome, String categoria, String descricao, BigDecimal valorDiariaBase, int quantidadeMinima, Integer quantidadeMaxima) {
        if (nome == null || nome.isBlank() || categoria == null || categoria.isBlank()) throw new IllegalArgumentException("Nome e categoria são obrigatórios.");
        if (descricao == null || descricao.isBlank()) throw new IllegalArgumentException("As especificações são obrigatórias.");
        if (valorDiariaBase == null || valorDiariaBase.signum() <= 0) throw new IllegalArgumentException("O valor da diária deve ser maior que zero.");
        if (quantidadeMinima < 0 || (quantidadeMaxima != null && quantidadeMaxima <= quantidadeMinima)) throw new IllegalArgumentException("A quantidade máxima deve ser maior que a mínima, e a mínima não pode ser negativa.");
        this.nome = nome.strip();
        this.categoria = categoria.strip();
        this.descricao = descricao;
        this.valorDiariaBase = valorDiariaBase;
        this.quantidadeMinima = quantidadeMinima;
        this.quantidadeMaxima = quantidadeMaxima;
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}