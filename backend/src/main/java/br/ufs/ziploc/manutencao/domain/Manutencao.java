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

    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Column(name = "tecnico_id", nullable = false)
    private Long tecnicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoManutencao tipo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "custo_gerado", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoGerado;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_prevista")
    private Instant dataPrevista;

    @Column(name = "data_conclusao")
    private Instant dataConclusao;

    @Column(name = "justificativa_atraso")
    private String justificativaAtraso;

    public Manutencao(Long locadoraId, Long unidadeId, Long tecnicoId, TipoManutencao tipo,
                      String descricao, Instant dataPrevista, Instant dataInicio) {
        if (locadoraId == null) throw new IllegalArgumentException("A locadora é obrigatória.");
        if (unidadeId == null) throw new IllegalArgumentException("A unidade é obrigatória.");
        if (tecnicoId == null) throw new IllegalArgumentException("O técnico é obrigatório.");
        if (tipo == null) throw new IllegalArgumentException("O tipo da manutenção é obrigatório.");
        if (descricao == null || descricao.isBlank()) throw new IllegalArgumentException("A descrição é obrigatória.");
        if (dataInicio == null) throw new IllegalArgumentException("A data de início é obrigatória.");
        if (dataPrevista != null && dataPrevista.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data prevista não pode ser anterior ao início.");
        }
        this.locadoraId = locadoraId;
        this.unidadeId = unidadeId;
        this.tecnicoId = tecnicoId;
        this.tipo = tipo;
        this.descricao = descricao.strip();
        this.dataPrevista = dataPrevista;
        this.dataInicio = dataInicio;
        this.custoGerado = BigDecimal.ZERO;
    }

    public boolean estaConcluida() {
        return dataConclusao != null;
    }

    public void concluir(Instant dataConclusao, BigDecimal custoGerado, String justificativaAtraso) {
        if (estaConcluida()) throw new IllegalStateException("A manutenção " + id + " já foi concluída.");
        if (dataConclusao == null) throw new IllegalArgumentException("A data de conclusão é obrigatória.");
        if (dataConclusao.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data de conclusão não pode ser anterior ao início.");
        }
        if (custoGerado == null || custoGerado.signum() < 0) {
            throw new IllegalArgumentException("O custo gerado não pode ser negativo.");
        }

        String justificativa = (justificativaAtraso == null || justificativaAtraso.isBlank())
                ? null : justificativaAtraso.strip();
        if (dataPrevista != null && dataConclusao.isAfter(dataPrevista) && justificativa == null) {
            throw new IllegalArgumentException(
                    "RD023: a conclusão após a data prevista exige justificativa do atraso.");
        }

        this.dataConclusao = dataConclusao;
        this.custoGerado = custoGerado;
        this.justificativaAtraso = justificativa;
    }
}