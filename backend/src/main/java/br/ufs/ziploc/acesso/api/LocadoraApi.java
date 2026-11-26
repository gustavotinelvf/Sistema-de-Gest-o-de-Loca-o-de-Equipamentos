package br.ufs.ziploc.acesso.api;

/**
 * Única porta de entrada dos outros módulos para os dados da locadora (D2).
 * Quem precisa do percentual de multa ou do raio de entrega chama este método
 * e recebe um record: nunca a entidade.
 */
public interface LocadoraApi {

    /** Devolve os parâmetros da locadora; falha se a carga inicial ainda não rodou. */
    ParametrosLocadoraDTO obterParametros();
}
