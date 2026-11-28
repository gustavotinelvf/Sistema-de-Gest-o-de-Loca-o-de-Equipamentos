package br.ufs.ziploc.equipamento.api;

import java.util.Optional;

/**
 * Porta de entrada do módulo equipamento (UnidadeApi, conforme o plano de projeto) para os demais módulos (monólito modular).
 * Outros módulos não devem usar os repositórios nem as entidades de equipamento diretamente.
 *
 * <p>As operações de status participam da transação do chamador e lançam:
 * RecursoNaoEncontradoException (unidade inexistente) e IllegalStateException (transição inválida).
 */
public interface UnidadeApi {

    Optional<Long> buscarUnidadeIdPorNumeroSerie(String numeroSerie);

    Optional<UnidadeInfo> buscarUnidade(Long unidadeId);

    /** Consulta a unidade com bloqueio pessimista na transação do chamador. */
    Optional<UnidadeInfo> buscarUnidadeParaAtualizacao(Long unidadeId);

    Optional<ModeloInfo> buscarModelo(Long modeloId);

    /** LOCADA → EM_VISTORIA (devolução recebida). */
    void iniciarVistoria(Long unidadeId);

    /** DISPONIVEL ou EM_VISTORIA → EM_MANUTENCAO. */
    void iniciarManutencao(Long unidadeId);

    /** EM_VISTORIA ou EM_MANUTENCAO → DISPONIVEL. */
    void liberar(Long unidadeId);

    /** DISPONIVEL → LOCADA (retirada do contrato). */
    void locar(Long unidadeId);
}