package br.ufs.ziploc.equipamento.service;

import br.ufs.ziploc.equipamento.domain.ModeloEquipamento;
import br.ufs.ziploc.equipamento.domain.StatusUnidade;
import br.ufs.ziploc.equipamento.domain.UnidadeEquipamento;
import br.ufs.ziploc.equipamento.repository.ModeloEquipamentoRepository;
import br.ufs.ziploc.equipamento.repository.UnidadeEquipamentoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadeApiImplTest {

    @Mock UnidadeEquipamentoRepository unidades;
    @Mock ModeloEquipamentoRepository modelos;
    @InjectMocks UnidadeApiImpl api;

    private UnidadeEquipamento unidade() {
        return new UnidadeEquipamento(new ModeloEquipamento(1L, "Betoneira", "Construção", "Betoneira de 400 litros", new BigDecimal("100.00"), 1, 5), "SN-1");
    }

    @Test
    void ciclo_locar_vistoria_manutencao_liberar() {
        UnidadeEquipamento u = unidade();
        when(unidades.findByIdForUpdate(1L)).thenReturn(Optional.of(u));

        api.locar(1L);
        assertThat(u.getStatus()).isEqualTo(StatusUnidade.LOCADA);
        api.iniciarVistoria(1L);
        assertThat(u.getStatus()).isEqualTo(StatusUnidade.EM_VISTORIA);
        api.iniciarManutencao(1L);
        assertThat(u.getStatus()).isEqualTo(StatusUnidade.EM_MANUTENCAO);
        api.liberar(1L);
        assertThat(u.getStatus()).isEqualTo(StatusUnidade.DISPONIVEL);
    }

    @Test
    void transicaoInvalidaLancaIllegalState() {
        UnidadeEquipamento u = unidade();
        when(unidades.findByIdForUpdate(1L)).thenReturn(Optional.of(u));

        // DISPONIVEL -> EM_VISTORIA não é permitido (só LOCADA -> EM_VISTORIA)
        assertThatThrownBy(() -> api.iniciarVistoria(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unidadeInexistenteLancaNaoEncontrado() {
        when(unidades.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.liberar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscaIdPorNumeroSerieIgnoraEspacos() {
        UnidadeEquipamento u = unidade();
        ReflectionTestUtils.setField(u, "id", 42L); // sem banco o id não é gerado
        when(unidades.findByNumeroSerieIgnoreCase("SN-1")).thenReturn(Optional.of(u));

        assertThat(api.buscarUnidadeIdPorNumeroSerie("  SN-1 ")).contains(42L);
        assertThat(api.buscarUnidadeIdPorNumeroSerie("  ")).isEmpty();
    }
}