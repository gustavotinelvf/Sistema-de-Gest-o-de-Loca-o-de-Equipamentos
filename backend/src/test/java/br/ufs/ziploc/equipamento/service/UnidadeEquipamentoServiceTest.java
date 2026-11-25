package br.ufs.ziploc.equipamento.service;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoRequest;
import br.ufs.ziploc.equipamento.dto.UnidadeEquipamentoResponse;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadeEquipamentoServiceTest {

    @Mock UnidadeEquipamentoRepository unidades;
    @Mock ModeloEquipamentoRepository modelos;
    @InjectMocks UnidadeEquipamentoService service;

    private ModeloEquipamento modelo() {
        return new ModeloEquipamento("Betoneira 400L", "Construção", null, new BigDecimal("120.00"), 1, 5);
    }

    @Test
    void criaUnidadeDisponivelComSerieSemEspacos() {
        when(modelos.findById(1L)).thenReturn(Optional.of(modelo()));
        when(unidades.existsByNumeroSerieIgnoreCase("SN-001")).thenReturn(false);
        when(unidades.saveAndFlush(any(UnidadeEquipamento.class))).thenAnswer(i -> i.getArgument(0));

        UnidadeEquipamentoResponse r = service.criar(new UnidadeEquipamentoRequest(1L, "  SN-001 "));

        assertThat(r.numeroSerie()).isEqualTo("SN-001");
        assertThat(r.status()).isEqualTo(StatusUnidade.DISPONIVEL);
        assertThat(r.ativo()).isTrue();
        assertThat(r.categoria()).isEqualTo("Construção");
    }

    @Test
    void rejeitaNumeroSerieDuplicado_RD009() {
        when(modelos.findById(1L)).thenReturn(Optional.of(modelo()));
        when(unidades.existsByNumeroSerieIgnoreCase("SN-001")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new UnidadeEquipamentoRequest(1L, "SN-001")))
                .isInstanceOf(ConflitoException.class);
        verify(unidades, never()).saveAndFlush(any());
    }

    @Test
    void traduzCorridaDeUniqueConstraintParaConflito() {
        when(modelos.findById(1L)).thenReturn(Optional.of(modelo()));
        when(unidades.existsByNumeroSerieIgnoreCase("SN-001")).thenReturn(false);
        when(unidades.saveAndFlush(any(UnidadeEquipamento.class)))
                .thenThrow(new DataIntegrityViolationException("dup", new SQLException("duplicate key", "23505")));

        assertThatThrownBy(() -> service.criar(new UnidadeEquipamentoRequest(1L, "SN-001")))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoCadastraUnidadeParaModeloInativo() {
        ModeloEquipamento inativo = modelo();
        inativo.inativar();
        when(modelos.findById(1L)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.criar(new UnidadeEquipamentoRequest(1L, "SN-001")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void alterarSerieParaUmaJaUsadaPorOutraUnidadeDaConflito() {
        UnidadeEquipamento u = new UnidadeEquipamento(modelo(), "SN-001");
        when(unidades.findById(7L)).thenReturn(Optional.of(u));
        when(unidades.existsByNumeroSerieIgnoreCaseAndIdNot("SN-002", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.alterarNumeroSerie(7L, "SN-002")).isInstanceOf(ConflitoException.class);
        assertThat(u.getNumeroSerie()).isEqualTo("SN-001");
    }

    @Test
    void naoInativaUnidadeLocada() {
        UnidadeEquipamento u = new UnidadeEquipamento(modelo(), "SN-001");
        u.locar();
        when(unidades.findById(7L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.inativar(7L)).isInstanceOf(IllegalStateException.class);
        assertThat(u.isAtivo()).isTrue();
    }

    @Test
    void naoReativaUnidadeDeModeloInativo() {
        ModeloEquipamento m = modelo();
        UnidadeEquipamento u = new UnidadeEquipamento(m, "SN-001");
        u.inativar();
        m.inativar();
        when(unidades.findById(7L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.reativar(7L)).isInstanceOf(IllegalStateException.class);
        assertThat(u.isAtivo()).isFalse();
    }
}
