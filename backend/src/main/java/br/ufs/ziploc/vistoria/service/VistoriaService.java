package br.ufs.ziploc.vistoria.service;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.dto.VistoriaDto;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VistoriaService {

    private static final String UNIDADE_EM_VISTORIA = "EM_VISTORIA";
    private static final String CONSTRAINT_DEVOLUCAO_UNICA = "vistoria_devolucao_id_key";

    private final VistoriaRepository vistorias;
    private final DevolucaoApi devolucaoApi;
    private final ContratoApi contratoApi;
    private final UnidadeApi unidadeApi;
    private final ManutencaoApi manutencaoApi;

    @Transactional
    public VistoriaDto registrar(Long locadoraId, Long devolucaoId, ResultadoVistoria resultado,
                                 String descricaoAvarias, Long tecnicoId) {

        validarId(locadoraId, "O identificador da locadora é inválido.");
        validarId(devolucaoId, "O identificador da devolução é inválido.");
        validarId(tecnicoId, "O identificador do técnico é inválido.");

        if (resultado == null) {
            throw new IllegalArgumentException("O resultado da vistoria é obrigatório.");
        }

        String descricao = descricaoAvarias == null || descricaoAvarias.isBlank()
                ? null : descricaoAvarias.strip();

        if (resultado == ResultadoVistoria.REPROVADA && descricao == null) {
            throw new IllegalArgumentException(
                    "RD022: a descrição das avarias é obrigatória quando a vistoria é reprovada.");
        }

        if (descricao != null && descricao.length() > 2000) {
            throw new IllegalArgumentException(
                    "A descrição das avarias deve possuir no máximo 2000 caracteres.");
        }

        // Serializa as tentativas de vistoria da mesma devolução
        // e restringe a consulta à locadora configurada.
        DevolucaoResumo devolucao = devolucaoApi.buscarPorIdParaAtualizacao(devolucaoId, locadoraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Devolução " + devolucaoId + " não encontrada."));

        Vistoria existente = vistorias.findByDevolucaoId(devolucaoId).orElse(null);

        if (existente != null) {
            if (!Objects.equals(existente.getLocadoraId(), locadoraId)) {
                throw new RecursoNaoEncontradoException(
                        "Devolução " + devolucaoId + " não encontrada.");
            }

            if (existente.estaConcluida()) {
                throw new IllegalStateException(
                        "A vistoria da devolução " + devolucaoId + " já foi concluída.");
            }

            if (!Objects.equals(existente.getTecnicoId(), tecnicoId)) {
                throw new IllegalStateException(
                        "A vistoria deve ser concluída pelo técnico responsável pelo seu início.");
            }
        }

        ItemContratoPrazo item = contratoApi.buscarItemContrato(devolucao.itemContratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Item de contrato " + devolucao.itemContratoId() + " não encontrado."));

        // Mantém a unidade bloqueada até a conclusão da transação.
        UnidadeInfo unidade = unidadeApi.buscarUnidadeParaAtualizacao(item.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade de equipamento " + item.unidadeId() + " não encontrada."));

        if (!UNIDADE_EM_VISTORIA.equals(unidade.status())) {
            throw new IllegalStateException(
                    "A unidade " + unidade.numeroSerie()
                            + " não está em vistoria (status atual: " + unidade.status() + ").");
        }

        Instant agora = Instant.now();

        if (agora.isBefore(devolucao.dataDevolucaoReal())) {
            throw new IllegalStateException(
                    "A vistoria não pode ser registrada antes da devolução.");
        }

        Vistoria vistoria = existente == null
                ? new Vistoria(locadoraId, devolucaoId, tecnicoId, agora)
                : existente;

        if (vistoria.getDataInicio().isBefore(devolucao.dataDevolucaoReal())) {
            throw new IllegalStateException(
                    "A data de início da vistoria é anterior à devolução.");
        }

        // A entidade somente é modificada após todas as consultas e validações.
        vistoria.concluir(resultado, descricao, agora);

        Vistoria salva;

        try {
            salva = vistorias.saveAndFlush(vistoria);
        } catch (DataIntegrityViolationException e) {
            if (violacaoDevolucaoUnica(e)) {
                throw new ConflitoException(
                        "Já existe vistoria para a devolução " + devolucaoId + ".");
            }

            throw e;
        }

        // A trigger do banco altera o status da unidade.
        // A manutenção corretiva participa da mesma transação.
        if (salva.reprovada()) {
            manutencaoApi.abrirAPartirDeVistoria(
                    locadoraId,
                    item.unidadeId(),
                    salva.getTecnicoId(),
                    salva.getDescricaoAvarias()
            );
        }

        return VistoriaDto.de(salva);
    }

    private static boolean violacaoDevolucaoUnica(Throwable erro) {
        for (Throwable causa = erro; causa != null;
             causa = causa.getCause() == causa ? null : causa.getCause()) {

            if (causa instanceof ConstraintViolationException violacao
                    && "23505".equals(violacao.getSQLState())
                    && CONSTRAINT_DEVOLUCAO_UNICA.equals(violacao.getConstraintName())) {
                return true;
            }
        }

        return false;
    }

    private static void validarId(Long id, String mensagem) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}