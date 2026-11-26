package br.ufs.ziploc.cliente.api;

import br.ufs.ziploc.cliente.domain.StatusAdimplencia;

//Dados resumidos do cliente expostos para outros módulos

public record ClienteResumoDTO(
        Long id,
        Long locadoraId,
        String razaoSocial,
        String cnpj,
        String representanteNome,
        String representanteCpf,
        String email,
        String telefone,
        StatusAdimplencia statusAdimplencia,
        boolean ativo,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {
}