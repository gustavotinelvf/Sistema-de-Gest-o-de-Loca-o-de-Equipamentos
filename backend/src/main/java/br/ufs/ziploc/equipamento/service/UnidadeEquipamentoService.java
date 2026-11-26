package br.ufs.ziploc.equipamento.service;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoRequest;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoResponse;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoSpecs;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.shared.persistence.ViolacaoIntegridade;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadeEquipamentoService {

    private final UnidadeEquipamentoRepository unidades;
    private final ModeloEquipamentoRepository modelos;

    @Transactional
    public UnidadeEquipamentoResponse criar(UnidadeEquipamentoRequest r) {
        ModeloEquipamento modelo = modelos.findById(r.modeloId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo de equipamento " + r.modeloId() + " não encontrado."));
        if (!modelo.isAtivo()) {
            throw new IllegalStateException("Não é possível cadastrar unidades para um modelo inativo.");
        }
        String numeroSerie = r.numeroSerie().strip();
        if (unidades.existsByNumeroSerieIgnoreCase(numeroSerie)) throw serieDuplicada(numeroSerie);
        return UnidadeEquipamentoResponse.de(salvar(new UnidadeEquipamento(modelo, numeroSerie)));
    }

    @Transactional
    public UnidadeEquipamentoResponse alterarNumeroSerie(Long id, String novoNumeroSerie) {
        UnidadeEquipamento unidade = carregarParaAtualizacao(id);
        String numeroSerie = novoNumeroSerie.strip();
        if (unidades.existsByNumeroSerieIgnoreCaseAndIdNot(numeroSerie, id)) throw serieDuplicada(numeroSerie);
        unidade.alterarNumeroSerie(numeroSerie);
        return UnidadeEquipamentoResponse.de(salvar(unidade));
    }

    /** Só unidades DISPONIVEL podem ser inativadas (regra da entidade). */
    @Transactional
    public UnidadeEquipamentoResponse inativar(Long id) {
        UnidadeEquipamento unidade = carregarParaAtualizacao(id);
        unidade.inativar();
        return UnidadeEquipamentoResponse.de(unidade);
    }

    @Transactional
    public UnidadeEquipamentoResponse reativar(Long id) {
        UnidadeEquipamento unidade = carregarParaAtualizacao(id);
        if (!unidade.getModelo().isAtivo()) {
            throw new IllegalStateException("Reative o modelo '" + unidade.getModelo().getNome() + "' antes de reativar a unidade.");
        }
        unidade.reativar();
        return UnidadeEquipamentoResponse.de(unidade);
    }

    @Transactional(readOnly = true)
    public UnidadeEquipamentoResponse buscar(Long id) {
        return UnidadeEquipamentoResponse.de(carregar(id));
    }

    @Transactional(readOnly = true)
    public List<UnidadeEquipamentoResponse> listar(Long modeloId, String categoria, StatusUnidade status, Boolean ativo) {
        return unidades.findAll(UnidadeEquipamentoSpecs.filtrar(modeloId, categoria, status, ativo), Sort.by("numeroSerie")).stream()
                .map(UnidadeEquipamentoResponse::de)
                .toList();
    }

    private UnidadeEquipamento carregar(Long id) {
        return unidades.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade de equipamento " + id + " não encontrada."));
    }

    private UnidadeEquipamento carregarParaAtualizacao(Long id) {
        return unidades.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade de equipamento " + id + " não encontrada."));
    }

    /** O flush antecipa a unique constraint do banco, que protege contra números de série idênticos (RD009). */
    private UnidadeEquipamento salvar(UnidadeEquipamento unidade) {
        try {
            return unidades.saveAndFlush(unidade);
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoIntegridade.temSqlState(e, ViolacaoIntegridade.UNICA)) throw serieDuplicada(unidade.getNumeroSerie());
            throw e;
        }
    }

    private static ConflitoException serieDuplicada(String numeroSerie) {
        return new ConflitoException("Já existe uma unidade com o número de série '" + numeroSerie + "' (RD009).");
    }
}