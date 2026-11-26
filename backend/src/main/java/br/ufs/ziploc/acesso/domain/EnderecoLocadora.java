package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Endereço da sede da locadora. Não é tabela própria: as colunas ficam dentro
 * da tabela "locadora" (@Embeddable), com o sufixo "_sede" do schema do Michael.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EnderecoLocadora {

    @Column(name = "cep_sede", nullable = false, length = 8)
    private String cep;

    @Column(name = "logradouro_sede", nullable = false, length = 200)
    private String logradouro;

    @Column(name = "numero_sede", nullable = false, length = 20)
    private String numero;

    @Column(name = "complemento_sede", length = 100)
    private String complemento;

    @Column(name = "bairro_sede", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade_sede", nullable = false, length = 100)
    private String cidade;

    @Column(name = "uf_sede", nullable = false, length = 2)
    private String uf;
}
