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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


// Cliente pessoa jurídica (locatário) atendido pela locadora

@Entity
@Table(name = "cliente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, length = 14, unique = true)
    private String cnpj;

    @Embedded
    private Contato contato;

    @Embedded
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_adimplencia", nullable = false, length = 15)
    private StatusAdimplencia statusAdimplencia = StatusAdimplencia.ADIMPLENTE;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    public Cliente(String razaoSocial, String cnpj, Contato contato, Endereco endereco) {
        this.razaoSocial = Objects.requireNonNull(razaoSocial, "Razão social é obrigatória");
        this.cnpj = Objects.requireNonNull(cnpj, "CNPJ é obrigatório");
        this.contato = Objects.requireNonNull(contato, "Contato é obrigatório");
        this.endereco = Objects.requireNonNull(endereco, "Endereço é obrigatório");
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

    public void atualizarDados(String razaoSocial, Contato contato, Endereco endereco) {
        this.razaoSocial = Objects.requireNonNull(razaoSocial, "Razão social é obrigatória");
        this.contato = Objects.requireNonNull(contato, "Contato é obrigatório");
        this.endereco = Objects.requireNonNull(endereco, "Endereço é obrigatório");
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public void alterarStatusAdimplencia(StatusAdimplencia novoStatus) {
        this.statusAdimplencia = Objects.requireNonNull(novoStatus, "Status de adimplência é obrigatório");
    }

    public void anonimizarDados() {
        String sufixoUnico = (this.id != null) ? this.id.toString() : UUID.randomUUID().toString();
        this.contato = new Contato(
                "ANONIMIZADO",
                "anonimizado-" + sufixoUnico + "@ziploc.invalid",
                "0000000000"
        );
        this.anonimizadoEm = Instant.now();
    }
}