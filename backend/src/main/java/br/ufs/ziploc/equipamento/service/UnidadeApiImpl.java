package br.ufs.ziploc.equipamento.service;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.ModeloInfo;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnidadeApiImpl implements UnidadeApi {

    private final UnidadeEquipamentoRepository unidades;
    private final ModeloEquipamentoRepository modelos;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarUnidadeIdPorNumeroSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) return Optional.empty();
        return unidades.findByNumeroSerieIgnoreCase(numeroSerie.strip()).map(UnidadeEquipamento::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UnidadeInfo> buscarUnidade(Long unidadeId) {
        return unidades.findById(unidadeId).map(u -> new UnidadeInfo(u.getId(), u.getModelo().getId(), u.getNumeroSerie(),
                u.getStatus().name(), u.isAtivo(), u.getModelo().getValorDiariaBase()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModeloInfo> buscarModelo(Long modeloId) {
        return modelos.findById(modeloId).map(m -> new ModeloInfo(m.getId(), m.getNome(), m.getCategoria(),
                m.getValorDiariaBase(), m.getQuantidadeMinima(), m.getQuantidadeMaxima(), m.isAtivo()));
    }

    @Override
    @Transactional
    public void iniciarVistoria(Long unidadeId) {
        carregar(unidadeId).iniciarVistoria();
        unidades.flush();
    }

    @Override
    @Transactional
    public void iniciarManutencao(Long unidadeId) {
        carregar(unidadeId).iniciarManutencao();
        unidades.flush();
    }

    @Override
    @Transactional
    public void liberar(Long unidadeId) {
        carregar(unidadeId).liberar();
        unidades.flush();
    }

    @Override
    @Transactional
    public void locar(Long unidadeId) {
        carregar(unidadeId).locar();
        unidades.flush();
    }

    private UnidadeEquipamento carregar(Long unidadeId) {
        return unidades.findByIdForUpdate(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade de equipamento " + unidadeId + " não encontrada."));
    }
}