package br.ufs.ziploc.cliente.dto;

import br.ufs.ziploc.integracao.cep.EnderecoCep;

/** Resposta de GET /api/clientes/cep/{cep}: o front usa para pré-preencher o formulário. */
public record CepResponseDTO(String cep, String logradouro, String bairro, String cidade, String uf) {

    public static CepResponseDTO from(EnderecoCep e) {
        return new CepResponseDTO(e.cep(), e.logradouro(), e.bairro(), e.cidade(), e.uf());
    }
}
