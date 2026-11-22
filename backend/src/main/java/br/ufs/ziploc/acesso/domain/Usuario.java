package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Usuário do sistema. Cargo = perfil. CPF obrigatório só para GERENTE (RD016).
 * A senha nunca é guardada em texto: aqui só entra o hash BCrypt.
 */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StatusUsuario status;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    @Column(name = "data_ativacao")
    private Instant dataAtivacao;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    /** Cria um usuário já ATIVO (o cadastro só existe depois do aceite do convite ou da carga inicial). */
    public static Usuario criarAtivo(String nome, String email, String senhaHash,
                                     Perfil perfil, String cpf) {
        if (perfil == Perfil.GERENTE && (cpf == null || cpf.isBlank())) {
            throw new IllegalArgumentException("CPF é obrigatório para o perfil GERENTE (RD016)");
        }
        Usuario u = new Usuario();
        u.nome = nome;
        u.email = email;
        u.senhaHash = senhaHash;
        u.perfil = perfil;
        u.cpf = (cpf == null || cpf.isBlank()) ? null : cpf;
        u.status = StatusUsuario.ATIVO;
        u.dataAtivacao = Instant.now();
        return u;
    }

    public void ativar() {
        this.status = StatusUsuario.ATIVO;
        if (this.dataAtivacao == null) {
            this.dataAtivacao = Instant.now();
        }
    }

    public void inativar() {
        this.status = StatusUsuario.INATIVO;
    }

    public boolean isAtivo() {
        return status == StatusUsuario.ATIVO;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCadastro == null) {
            dataCadastro = Instant.now();
        }
    }
}
