package br.ufs.ziploc.cliente.dto;

import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.Contato;
import br.ufs.ziploc.cliente.domain.Endereco;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import java.time.Instant;


//Representação HTTP retornada nos endpoints de cliente

public record ClienteResponseDTO(
        Long id,
        Long locadoraId,
        String razaoSocial,
        String cnpj,
        String representanteNome,
        String representanteCpf,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        StatusAdimplencia statusAdimplencia,
        boolean ativo,
        Instant dataCadastro,
        Instant dataAnonimizacao
) {
    public static ClienteResponseDTO de(Cliente cliente) {
        Contato contato = cliente.getContato();
        Endereco endereco = cliente.getEndereco();
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getLocadoraId(),
                cliente.getRazaoSocial(),
                cliente.getCnpj(),
                contato != null ? contato.getNome() : null,
                contato != null ? contato.getCpf() : null,
                contato != null ? contato.getEmail() : null,
                contato != null ? contato.getTelefone() : null,
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                cliente.getStatusAdimplencia(),
                cliente.isAtivo(),
                cliente.getDataCadastro(),
                cliente.getDataAnonimizacao()
        );
    }
}