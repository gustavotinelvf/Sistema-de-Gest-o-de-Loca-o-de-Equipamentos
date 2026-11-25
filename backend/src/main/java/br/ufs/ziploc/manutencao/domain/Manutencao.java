package br.ufs.ziploc.manutencao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "manutencao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    /** Nulo = manutenção avulsa (o técnico escolhe a unidade pelo número de série). */
    @Column(name = "vistoria_id")
    private Long vistoriaId;

    @Column(name = "usuario_tecnico_id", nullable = false)
    private Long usuarioTecnicoId;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "descricao_problema", nullable = false)
    private String descricaoProblema;

    @Column(name = "custo_reparo", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoReparo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusManutencao status;

    public Manutencao(Long unidadeId, Long vistoriaId, Long usuarioTecnicoId,
                      String descricaoProblema, Instant dataInicio) {
        if (unidadeId == null) throw new IllegalArgumentException("A unidade é obrigatória.");
        if (usuarioTecnicoId == null) throw new IllegalArgumentException("O técnico é obrigatório.");
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new IllegalArgumentException("A descrição do problema é obrigatória.");
        }
        if (dataInicio == null) throw new IllegalArgumentException("A data de início é obrigatória.");
        this.unidadeId = unidadeId;
        this.vistoriaId = vistoriaId;
        this.usuarioTecnicoId = usuarioTecnicoId;
        this.descricaoProblema = descricaoProblema.strip();
        this.dataInicio = dataInicio;
        this.custoReparo = BigDecimal.ZERO;
        this.status = StatusManutencao.EM_ANDAMENTO;
    }

    public boolean estaConcluida() {
        return status == StatusManutencao.CONCLUIDA;
    }

    public void concluir(Instant dataFim, BigDecimal custoReparo) {
        if (estaConcluida()) throw new IllegalStateException("A manutenção " + id + " já foi concluída.");
        if (dataFim == null) throw new IllegalArgumentException("A data de fim é obrigatória.");
        if (dataFim.isBefore(dataInicio)) throw new IllegalArgumentException("A data de fim não pode ser anterior ao início.");
        if (custoReparo == null || custoReparo.signum() < 0) {
            throw new IllegalArgumentException("O custo do reparo não pode ser negativo.");
        }
        this.dataFim = dataFim;
        this.custoReparo = custoReparo;
        this.status = StatusManutencao.CONCLUIDA;
    }
}