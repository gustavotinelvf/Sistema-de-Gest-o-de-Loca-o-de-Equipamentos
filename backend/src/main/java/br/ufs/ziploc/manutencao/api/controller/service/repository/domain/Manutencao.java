package br.ufs.ziploc.manutencao.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Column(name = "vistoria_id") // nulo = manutenção isolada, sem vinculo com a vistoria
    private Long vistoriaId;

    @Column(name = "usuario_tecnico_id", nullable = false)
    private Long usuarioTecnicoId;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "descricao_problema", nullable = false)
    private String descricaoProblema;

    @Column(name = "custo_reparo", nullable = false)
    private BigDecimal custoReparo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusManutencao status;

    protected Manutencao() { } // exigido pelo JPA

    public static Manutencao nova(Long unidadeId, Long vistoriaId,
                                  Long usuarioTecnicoId, String descricaoProblema) {
        Manutencao m = new Manutencao();
        m.unidadeId = unidadeId;
        m.vistoriaId = vistoriaId;
        m.usuarioTecnicoId = usuarioTecnicoId;
        m.descricaoProblema = descricaoProblema;
        m.custoReparo = BigDecimal.ZERO;
        m.iniciar();
        return m;
    }

    public void iniciar() {
        this.dataInicio = Instant.now();
        this.status = StatusManutencao.EM_ANDAMENTO;
    }

    public void concluir(Instant dataFim, BigDecimal custoReparo) {
        this.dataFim = dataFim;
        this.custoReparo = custoReparo;
        this.status = StatusManutencao.CONCLUIDA;
    }

    public boolean estaConcluida() {
        return status == StatusManutencao.CONCLUIDA;
    }

    public Long getId() { return id; }
    public Long getUnidadeId() { return unidadeId; }
    public Long getVistoriaId() { return vistoriaId; }
    public Long getUsuarioTecnicoId() { return usuarioTecnicoId; }
    public Instant getDataInicio() { return dataInicio; }
    public Instant getDataFim() { return dataFim; }
    public String getDescricaoProblema() { return descricaoProblema; }
    public BigDecimal getCustoReparo() { return custoReparo; }
    public StatusManutencao getStatus() { return status; }
}