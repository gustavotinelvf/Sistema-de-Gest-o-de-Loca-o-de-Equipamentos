package br.ufs.ziploc.acesso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Endereço da sede da locadora. Não é tabela própria: as colunas ficam
 * dentro da tabela "locadora" (@Embeddable).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EnderecoLocadora {

    @Column(name = "cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "logradouro", nullable = false, length = 200)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    // A coluna é CHAR(2): sem o JdbcTypeCode, o ddl-auto=validate reclama
    // que esperava VARCHAR.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", nullable = false, length = 2)
    private String uf;
}
