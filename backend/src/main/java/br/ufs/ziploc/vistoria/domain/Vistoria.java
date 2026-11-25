package br.ufs.ziploc.vistoria.domain;

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

import java.time.Instant;

@Entity
@Table(name = "vistoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;

    @Column(name = "devolucao_id", nullable = false, unique = true)
    private Long devolucaoId;

    @Column(name = "tecnico_id", nullable = false)
    private Long tecnicoId;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_conclusao")
    private Instant dataConclusao;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 10)
    private ResultadoVistoria resultado;

    @Column(name = "descricao_avarias")
    private String descricaoAvarias;

    public Vistoria(Long locadoraId, Long devolucaoId, Long tecnicoId, Instant dataInicio) {
        if (locadoraId == null) throw new IllegalArgumentException("A locadora é obrigatória.");
        if (devolucaoId == null) throw new IllegalArgumentException("A devolução é obrigatória.");
        if (tecnicoId == null) throw new IllegalArgumentException("O técnico é obrigatório.");
        if (dataInicio == null) throw new IllegalArgumentException("A data de início é obrigatória.");
        this.locadoraId = locadoraId;
        this.devolucaoId = devolucaoId;
        this.tecnicoId = tecnicoId;
        this.dataInicio = dataInicio;
    }

    public boolean estaConcluida() {
        return dataConclusao != null;
    }

    public boolean reprovada() {
        return resultado == ResultadoVistoria.REPROVADA;
    }

    public void concluir(ResultadoVistoria resultado, String descricaoAvarias, Instant dataConclusao) {
        if (estaConcluida()) {
            throw new IllegalStateException("A vistoria da devolução " + devolucaoId + " já foi concluída.");
        }
        if (resultado == null) throw new IllegalArgumentException("O resultado da vistoria é obrigatório.");
        if (dataConclusao == null) throw new IllegalArgumentException("A data de conclusão é obrigatória.");
        if (dataConclusao.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data de conclusão não pode ser anterior ao início.");
        }

        String descricao = (descricaoAvarias == null || descricaoAvarias.isBlank()) ? null : descricaoAvarias.strip();
        if (resultado == ResultadoVistoria.REPROVADA && descricao == null) {
            throw new IllegalArgumentException(
                    "RD022: a descrição das avarias é obrigatória quando a vistoria é reprovada.");
        }

        this.resultado = resultado;
        this.descricaoAvarias = descricao;
        this.dataConclusao = dataConclusao;
    }
}