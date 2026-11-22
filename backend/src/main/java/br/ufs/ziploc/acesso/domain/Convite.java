package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Convite de acesso. Estados: PENDENTE -> ACEITO | EXPIRADO | CANCELADO.
 * Só convite PENDENTE pode mudar de estado (RD017).
 */
@Entity
@Table(name = "convite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "token", nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusConvite status;

    // Relação JPA só dentro do módulo acesso (regra D4).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convidado_por_id", nullable = false)
    private Usuario convidadoPor;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(name = "data_aceite")
    private Instant dataAceite;

    /** Cria um convite PENDENTE. O token (único) é gerado pelo service. */
    public static Convite criar(String email, Perfil perfil, Usuario convidadoPor,
                                String token, Instant dataExpiracao) {
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(perfil, "perfil é obrigatório");
        Objects.requireNonNull(convidadoPor, "convidadoPor é obrigatório");
        Objects.requireNonNull(token, "token é obrigatório");
        Objects.requireNonNull(dataExpiracao, "dataExpiracao é obrigatória");
        Instant agora = Instant.now();
        if (!dataExpiracao.isAfter(agora)) {
            throw new IllegalArgumentException(
                    "A data de expiração deve ser posterior à data de envio (RD017)");
        }
        Convite c = new Convite();
        c.email = email;
        c.perfil = perfil;
        c.convidadoPor = convidadoPor;
        c.token = token;
        c.status = StatusConvite.PENDENTE;
        c.dataCriacao = agora;
        c.dataExpiracao = dataExpiracao;
        return c;
    }

    public boolean isPendente() {
        return status == StatusConvite.PENDENTE;
    }

    public boolean isVencido(Instant agora) {
        return !agora.isBefore(dataExpiracao);
    }

    public void aceitar(Instant agora) {
        exigirPendente("aceitar");
        if (isVencido(agora)) {
            throw new IllegalStateException(
                    "Não é possível aceitar um convite vencido (RD017)");
        }
        this.status = StatusConvite.ACEITO;
        this.dataAceite = agora;
    }

    public void cancelar() {
        exigirPendente("cancelar");
        this.status = StatusConvite.CANCELADO;
    }

    public void expirar() {
        exigirPendente("expirar");
        this.status = StatusConvite.EXPIRADO;
    }

    private void exigirPendente(String acao) {
        if (status != StatusConvite.PENDENTE) {
            // O @RestControllerAdvice do shared converte em 422 (regra de negócio).
            throw new IllegalStateException(
                    "Não é possível " + acao + " um convite " + status + " (RD017)");
        }
    }

    @PrePersist
    void aoPersistir() {
        if (dataCriacao == null) {
            dataCriacao = Instant.now();
        }
    }
}