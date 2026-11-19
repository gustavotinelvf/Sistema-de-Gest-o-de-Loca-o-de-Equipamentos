package br.ufs.ziploc.devolucao.provisorio;


/* TODO: É PROVISÓRIO ATÉ TER A UNIDADE API DE FATO */
public interface UnidadeApi {

    void alterarStatus(Long unidadeId, StatusUnidade novoStatus);
}
