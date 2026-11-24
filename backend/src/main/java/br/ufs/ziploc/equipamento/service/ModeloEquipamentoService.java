package br.ufs.ziploc.equipamento.service;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.dto.ModeloEquipamentoRequest;
import br.ufs.ziploc.equipamento.dto.ModeloEquipamentoResponse;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoSpecs;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModeloEquipamentoService {

    private final ModeloEquipamentoRepository modelos;
    private final UnidadeEquipamentoRepository unidades;

    @Transactional
    public ModeloEquipamentoResponse criar(ModeloEquipamentoRequest r) {
        ModeloEquipamento modelo = new ModeloEquipamento(r.nome(), r.categoria(), r.descricao(),
                r.valorDiariaBase(), r.quantidadeMinima(), r.quantidadeMaxima());
        return ModeloEquipamentoResponse.de(modelos.save(modelo));
    }

    @Transactional
    public ModeloEquipamentoResponse atualizar(Long id, ModeloEquipamentoRequest r) {
        ModeloEquipamento modelo = carregar(id);
        modelo.atualizar(r.nome(), r.categoria(), r.descricao(), r.valorDiariaBase(), r.quantidadeMinima(), r.quantidadeMaxima());
        return ModeloEquipamentoResponse.de(modelos.save(modelo));
    }

    @Transactional
    public ModeloEquipamentoResponse inativar(Long id) {
        ModeloEquipamento modelo = carregar(id);
        if (unidades.existsByModeloIdAndStatus(id, StatusUnidade.LOCADA)) {
            throw new IllegalStateException("Não é possível inativar um modelo que possui unidades locadas.");
        }
        modelo.inativar();
        return ModeloEquipamentoResponse.de(modelo);
    }

    @Transactional
    public ModeloEquipamentoResponse reativar(Long id) {
        ModeloEquipamento modelo = carregar(id);
        modelo.reativar();
        return ModeloEquipamentoResponse.de(modelo);
    }

    @Transactional(readOnly = true)
    public ModeloEquipamentoResponse buscar(Long id) {
        return ModeloEquipamentoResponse.de(carregar(id));
    }

    /** Lista por nome; {@code categoria} (sem diferenciar maiúsculas) e {@code ativo} são filtros opcionais. */
    @Transactional(readOnly = true)
    public List<ModeloEquipamentoResponse> listar(String categoria, Boolean ativo) {
        return modelos.findAll(ModeloEquipamentoSpecs.filtrar(categoria, ativo), Sort.by("nome")).stream()
                .map(ModeloEquipamentoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return modelos.listarCategorias();
    }

    private ModeloEquipamento carregar(Long id) {
        return modelos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo de equipamento " + id + " não encontrado."));
    }
}
