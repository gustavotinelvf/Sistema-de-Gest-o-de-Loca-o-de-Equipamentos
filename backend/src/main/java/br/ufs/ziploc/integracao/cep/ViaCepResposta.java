package br.ufs.ziploc.integracao.cep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Formato da resposta do ViaCEP. CEP inexistente volta {"erro": true}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResposta(String logradouro, String bairro, String localidade, String uf, Boolean erro) {
}
