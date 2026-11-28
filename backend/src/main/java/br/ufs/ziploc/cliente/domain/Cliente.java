package br.ufs.ziploc.cliente.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

// Cliente pessoa jurídica (locatário) vinculado a uma locadora.

@Entity
@Table(name = "cliente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Regra D4: referência a outro módulo (acesso)
    // é guardada apenas pelo id.
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, length = 14, unique = true)
    private String cnpj;

    @Embedded
    private Contato contato;

    @Embedded
    private Endereco endereco;

    // RD010 e RF018: o status é atualizado pelo banco de dados
    // e pelo fluxo de reconsideração gerencial.
    // O Hibernate não deve sobrescrever essa classificação.
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status_adimplencia",
            nullable = false,
            length = 15,
            updatable = false
    )
    private StatusAdimplencia statusAdimplencia =
            StatusAdimplencia.ADIMPLENTE;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    @Column(name = "data_anonimizacao")
    private Instant dataAnonimizacao;

    public Cliente(
            Long locadoraId,
            String razaoSocial,
            String cnpj,
            Contato contato,
            Endereco endereco) {

        this.locadoraId = Objects.requireNonNull(
                locadoraId, "Locadora é obrigatória");

        this.razaoSocial = validarRazaoSocial(razaoSocial);

        this.cnpj = Objects.requireNonNull(
                cnpj, "CNPJ é obrigatório");

        this.contato = contato;

        this.endereco = Objects.requireNonNull(
                endereco, "Endereço é obrigatório");

        this.statusAdimplencia = StatusAdimplencia.ADIMPLENTE;

        this.ativo = true;
    }

    @PrePersist
    void aoPersistir() {

        if (dataCadastro == null) {
            dataCadastro = Instant.now();
        }

        if (statusAdimplencia == null) {
            statusAdimplencia = StatusAdimplencia.ADIMPLENTE;
        }
    }

    public void atualizarDados(
            String razaoSocial,
            Contato contato,
            Endereco endereco) {

        this.razaoSocial = validarRazaoSocial(razaoSocial);

        this.contato = contato;

        this.endereco = Objects.requireNonNull(
                endereco, "Endereço é obrigatório");
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public void alterarStatusAdimplencia(
            StatusAdimplencia novoStatus) {

        this.statusAdimplencia = Objects.requireNonNull(
                novoStatus, "Status de adimplência é obrigatório");
    }

    public void anonimizarDados() {

        // CPF, e-mail e telefone ficam nulos para remover
        // dados pessoais sem violar o CHECK de formato
        // de representante_cpf no banco.
        this.contato = new Contato(
                "ANONIMIZADO", null, null, null);

        this.dataAnonimizacao = Instant.now();
    }

    private static String validarRazaoSocial(String razaoSocial) {

        if (razaoSocial == null || razaoSocial.isBlank()) {

            throw new IllegalArgumentException(
                    "Razão social não pode estar em branco");
        }

        return razaoSocial.trim();
    }
}