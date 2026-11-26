package br.ufs.ziploc.equipamento.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unidade_equipamento", uniqueConstraints = @UniqueConstraint(columnNames = {"locadora_id", "numero_serie"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnidadeEquipamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private ModeloEquipamento modelo;
    @Column(name = "numero_serie", nullable = false, length = 50)
    private String numeroSerie;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusUnidade status;
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    public UnidadeEquipamento(ModeloEquipamento modelo, String numeroSerie) {
        if (modelo == null) throw new IllegalArgumentException("O modelo é obrigatório.");
        if (modelo.getLocadoraId() == null) throw new IllegalArgumentException("A locadora do modelo é obrigatória.");
        this.modelo = modelo;
        this.locadoraId = modelo.getLocadoraId();
        alterarNumeroSerie(numeroSerie);
        this.status = StatusUnidade.DISPONIVEL;
        this.ativo = true;
    }

    public void alterarNumeroSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) throw new IllegalArgumentException("O número de série é obrigatório.");
        this.numeroSerie = numeroSerie.strip();
    }

    public void locar() {
        if (!ativo) throw new IllegalStateException("Unidade inativa não pode ser locada.");
        transicionarPara(StatusUnidade.LOCADA);
    }

    public void iniciarVistoria() {
        transicionarPara(StatusUnidade.EM_VISTORIA);
    }

    public void iniciarManutencao() {
        transicionarPara(StatusUnidade.EM_MANUTENCAO);
    }

    public void liberar() {
        transicionarPara(StatusUnidade.DISPONIVEL);
    }

    public void inativar() {
        if (status != StatusUnidade.DISPONIVEL) throw new IllegalStateException("Só é possível inativar uma unidade disponível.");
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    private void transicionarPara(StatusUnidade destino) {
        if (!status.podeTransicionarPara(destino)) throw new IllegalStateException("Transição de status inválida: " + status + " → " + destino + ".");
        this.status = destino;
    }
}