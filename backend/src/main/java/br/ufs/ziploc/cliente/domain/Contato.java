package br.ufs.ziploc.cliente.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


//Dados de contato do cliente PJ. Não é tabela própria: as colunas ficam
//dentro da tabela "cliente" (@Embeddable)

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Contato {

    @Column(name = "contato_nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "email", nullable = false, length = 200, unique = true)
    private String email;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;
}