package br.ufs.ziploc.cliente.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

//Dados do representante e contato do cliente PJ
//Não é tabela própria: as colunas ficam dentro da tabela "cliente" (@Embeddable)

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Contato {

    @Column(name = "representante_nome", length = 200)
    private String nome;

    @Column(name = "representante_cpf", length = 11)
    private String cpf;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;
}