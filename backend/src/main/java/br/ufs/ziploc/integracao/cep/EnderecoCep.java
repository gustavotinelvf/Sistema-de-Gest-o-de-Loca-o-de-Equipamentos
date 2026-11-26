package br.ufs.ziploc.integracao.cep;

/** Endereço devolvido pela consulta de CEP (sem número e complemento: quem digita é o usuário). */
public record EnderecoCep(String cep, String logradouro, String bairro, String cidade, String uf) {
}
