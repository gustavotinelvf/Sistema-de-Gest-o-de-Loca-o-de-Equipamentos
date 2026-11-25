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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "vistoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devolucao_id", nullable = false, unique = true)
    private Long devolucaoId;

    @Column(name = "usuario_tecnico_id", nullable = false)
    private Long usuarioTecnicoId;

    @Column(name = "data_vistoria", nullable = false)
    private Instant dataVistoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusVistoria status;

    @Column(name = "custo_dano", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoDano;

    @Column(name = "laudo")
    private String laudo;

    public Vistoria(Long devolucaoId, Long usuarioTecnicoId, Instant dataVistoria,
                    StatusVistoria status, BigDecimal custoDano, String laudo) {
        if (devolucaoId == null) throw new IllegalArgumentException("A devolução é obrigatória.");
        if (usuarioTecnicoId == null) throw new IllegalArgumentException("O técnico é obrigatório.");
        if (dataVistoria == null) throw new IllegalArgumentException("A data da vistoria é obrigatória.");
        if (status == null) throw new IllegalArgumentException("O status da vistoria é obrigatório.");

        BigDecimal custo = custoDano == null ? BigDecimal.ZERO : custoDano.setScale(2, RoundingMode.HALF_UP);
        if (custo.signum() < 0) throw new IllegalArgumentException("O custo do dano não pode ser negativo.");

        String laudoLimpo = (laudo == null || laudo.isBlank()) ? null : laudo.strip();
        boolean reprovada = status == StatusVistoria.REPROVADA_COM_DANOS;
        if (reprovada && laudoLimpo == null) {
            throw new IllegalArgumentException("RD022: o laudo é obrigatório quando a vistoria é reprovada.");
        }
        if (!reprovada && custo.signum() > 0) {
            throw new IllegalArgumentException("Uma vistoria aprovada não pode ter custo de dano.");
        }

        this.devolucaoId = devolucaoId;
        this.usuarioTecnicoId = usuarioTecnicoId;
        this.dataVistoria = dataVistoria;
        this.status = status;
        this.custoDano = custo;
        this.laudo = laudoLimpo;
    }

    public boolean reprovada() {
        return status == StatusVistoria.REPROVADA_COM_DANOS;
    }
}