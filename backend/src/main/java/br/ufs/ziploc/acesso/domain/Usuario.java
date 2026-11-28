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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Usuário do sistema (tabela "usuario" do schema do Michael). Cargo = perfil.
 * Dois jeitos de nascer: ATIVO (carga inicial) ou PENDENTE (criado ao enviar o convite;
 * vira ATIVO no aceite, com senha). CPF obrigatório para GERENTE (RD016).
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

    @Column(name = "locadora_id", nullable = false, updatable = false)
    private Long locadoraId;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "cpf", length = 11)
    private String cpf;

    /** Nulo enquanto PENDENTE; o banco exige senha para status ATIVO. */
    @Column(name = "senha_hash", length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 30)
    private Perfil perfil;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusUsuario status;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    @Column(name = "data_ativacao")
    private Instant dataAtivacao;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    /** Cria um usuário já ATIVO (carga inicial). */
    public static Usuario criarAtivo(Long locadoraId, String nome, String email, String senhaHash,
                                     Perfil perfil, String cpf) {
        Objects.requireNonNull(locadoraId, "locadoraId é obrigatório");
        Objects.requireNonNull(senhaHash, "senhaHash é obrigatório para usuário ATIVO");
        exigirCpfSeGerente(perfil, cpf);
        Usuario u = new Usuario();
        u.locadoraId = locadoraId;
        u.nome = nome;
        u.email = email;
        u.senhaHash = senhaHash;
        u.perfil = perfil;
        u.cpf = normalizarCpf(cpf);
        u.status = StatusUsuario.ATIVO;
        u.dataAtivacao = Instant.now();
        return u;
    }

    /** Cria um usuário PENDENTE, sem senha: é o destinatário de um convite. */
    public static Usuario criarPendente(Long locadoraId, String nome, String email,
                                        Perfil perfil, String cargo) {
        Objects.requireNonNull(locadoraId, "locadoraId é obrigatório");
        Usuario u = new Usuario();
        u.locadoraId = locadoraId;
        u.nome = nome;
        u.email = email;
        u.perfil = perfil;
        u.cargo = cargo;
        u.status = StatusUsuario.PENDENTE;
        return u;
    }

    /** Aceite do convite: define a senha (e o CPF, se faltar) e ativa o usuário. */
    public void concluirCadastro(String senhaHash, String cpf, Instant agora) {
        if (status != StatusUsuario.PENDENTE) {
            throw new IllegalStateException("Só um usuário PENDENTE pode concluir o cadastro");
        }
        Objects.requireNonNull(senhaHash, "senhaHash é obrigatório");
        String cpfFinal = normalizarCpf(cpf) != null ? normalizarCpf(cpf) : this.cpf;
        exigirCpfSeGerente(perfil, cpfFinal);
        this.cpf = cpfFinal;
        this.senhaHash = senhaHash;
        this.status = StatusUsuario.ATIVO;
        this.dataAtivacao = agora;
    }

    /** Reativa um usuário que já teve senha, desde que não esteja anonimizado. */
    public void ativar() {
        if (isAnonimizado()) {
            throw new IllegalStateException("Um usuário anonimizado não pode ser reativado");
        }
        if (senhaHash == null) {
            throw new IllegalStateException("O usuário ainda não concluiu o cadastro (sem senha)");
        }
        exigirCpfSeGerente(perfil, cpf);
        this.status = StatusUsuario.ATIVO;
        if (this.dataAtivacao == null) {
            this.dataAtivacao = Instant.now();
        }
    }

    public void inativar() {
        this.status = StatusUsuario.INATIVO;
    }

    /**
     * Anonimização LGPD (tarefa 18, rotina EXTRA validada pelo Tech Lead): só vale para usuário
     * INATIVO ainda não anonimizado. Remove os dados pessoais e as credenciais, mantendo o registro
     * (e o id) para o histórico das operações. Respeita os CHECKs do banco: o anonimizado fica
     * INATIVO, sem CPF e sem senha, e o e-mail continua válido e único (usa o id).
     */
    public void anonimizar(Instant agora) {
        Objects.requireNonNull(agora, "A data da anonimização é obrigatória");
        if (isAnonimizado()) {
            throw new IllegalStateException("O usuário já foi anonimizado");
        }
        if (status != StatusUsuario.INATIVO) {
            throw new IllegalStateException("Só um usuário INATIVO pode ser anonimizado");
        }
        if (id == null) {
            throw new IllegalStateException("O usuário precisa estar salvo para ser anonimizado");
        }
        this.nome = "ANONIMIZADO";
        this.email = "anonimizado-" + id + "@anonimizado.invalid";
        this.cpf = null;
        this.senhaHash = null;
        this.cargo = null;
        this.anonimizadoEm = agora;
    }

    public boolean isAtivo() {
        return status == StatusUsuario.ATIVO;
    }

    public boolean isPendente() {
        return status == StatusUsuario.PENDENTE;
    }

    public boolean isAnonimizado() {
        return anonimizadoEm != null;
    }

    private static void exigirCpfSeGerente(Perfil perfil, String cpf) {
        if (perfil == Perfil.GERENTE && normalizarCpf(cpf) == null) {
            throw new IllegalArgumentException("CPF é obrigatório para o perfil GERENTE (RD016)");
        }
    }

    private static String normalizarCpf(String cpf) {
        return (cpf == null || cpf.isBlank()) ? null : cpf;
    }

    @PrePersist
    void aoPersistir() {
        if (dataCadastro == null) {
            dataCadastro = Instant.now();
        }
    }
}