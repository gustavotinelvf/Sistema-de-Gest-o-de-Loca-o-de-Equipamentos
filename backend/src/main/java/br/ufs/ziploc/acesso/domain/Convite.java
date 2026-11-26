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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Convite de acesso (schema do Michael): aponta para o usuário PENDENTE convidado e guarda
 * só o hash SHA-256 do token (o token em texto vai apenas no link do e-mail).
 * Estados: PENDENTE -> ACEITO | EXPIRADO | CANCELADO. Só convite PENDENTE muda de estado (RD017).
 */
@Entity
@Table(name = "convite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relação JPA só dentro do módulo acesso (regra D4).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusConvite status;

    @Column(name = "data_envio", nullable = false, updatable = false)
    private Instant dataEnvio;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(name = "data_aceite")
    private Instant dataAceite;

    /** Cria um convite PENDENTE para um usuário PENDENTE. tokenHash = hashDoToken(token). */
    public static Convite criar(Usuario usuario, String tokenHash, Instant dataExpiracao) {
        Objects.requireNonNull(usuario, "usuario é obrigatório");
        Objects.requireNonNull(tokenHash, "tokenHash é obrigatório");
        Objects.requireNonNull(dataExpiracao, "dataExpiracao é obrigatória");
        if (!usuario.isPendente()) {
            throw new IllegalArgumentException("O convite só pode ser enviado a um usuário PENDENTE");
        }
        if (!tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("tokenHash deve ter 64 caracteres hexadecimais (SHA-256)");
        }
        Instant agora = Instant.now();
        if (!dataExpiracao.isAfter(agora)) {
            throw new IllegalArgumentException(
                    "A data de expiração deve ser posterior à data de envio (RD017)");
        }
        Convite c = new Convite();
        c.usuario = usuario;
        c.tokenHash = tokenHash;
        c.status = StatusConvite.PENDENTE;
        c.dataEnvio = agora;
        c.dataExpiracao = dataExpiracao;
        return c;
    }

    /** SHA-256 em hexadecimal minúsculo (64 caracteres), como o banco exige. */
    public static String hashDoToken(String tokenEmTexto) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(tokenEmTexto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
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
        if (dataEnvio == null) {
            dataEnvio = Instant.now();
        }
    }
}
