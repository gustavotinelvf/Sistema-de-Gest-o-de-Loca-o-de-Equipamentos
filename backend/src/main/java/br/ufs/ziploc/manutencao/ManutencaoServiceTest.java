package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.equipamento.api.EquipamentoApi;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.manutencao.service.ManutencaoService;
import br.ufs.ziploc.shared.erro.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    @Mock ManutencaoRepository repository;
    @Mock UnidadeApi unidadeApi;
    @Mock EquipamentoApi equipamentoApi;
    @InjectMocks ManutencaoService service;

    @Test
    void iniciarAvulsa_numeroSerieInexistente_retorna404() {
        when(equipamentoApi.buscarUnidadeIdPorNumeroSerie("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarAvulsa("XXX", "defeito", 1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void iniciarAvulsa_unidadeJaEmManutencao_retorna409() {
        when(equipamentoApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        when(repository.existsByUnidadeIdAndStatusIn(eq(10L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.iniciarAvulsa("SN-1", "defeito", 1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        verify(unidadeApi, never()).alterarStatus(any(), any());
    }

    @Test
    void iniciarAvulsa_ok_mudaUnidadeParaEmManutencao() {
        when(equipamentoApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        when(repository.existsByUnidadeIdAndStatusIn(eq(10L), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.iniciarAvulsa("SN-1", "defeito", 1L);

        verify(unidadeApi).alterarStatus(10L, "EM_MANUTENCAO");
        assertThat(dto.vistoriaId()).isNull();
        assertThat(dto.status().name()).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void concluir_jaConcluida_retorna422() {
        Manutencao m = Manutencao.nova(10L, null, 1L, "defeito");
        m.concluir(java.time.Instant.now(), BigDecimal.ONE);
        when(repository.findById(5L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void concluir_ok_devolveUnidadeParaDisponivel() {
        Manutencao m = Manutencao.nova(10L, null, 1L, "defeito");
        when(repository.findById(5L)).thenReturn(Optional.of(m));

        var dto = service.concluir(5L, new BigDecimal("150.555"));

        verify(unidadeApi).alterarStatus(10L, "DISPONIVEL");
        assertThat(dto.custoReparo()).isEqualByComparingTo("150.56");
        assertThat(dto.dataFim()).isNotNull();
    }
}