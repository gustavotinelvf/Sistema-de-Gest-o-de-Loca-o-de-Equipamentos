package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.StatusManutencao;
import br.ufs.ziploc.manutencao.provisorio.DevolucaoApi;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.manutencao.service.ManutencaoService;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    private static final Instant INICIO = Instant.parse("2020-01-01T00:00:00Z");

    @Mock ManutencaoRepository manutencoes;
    @Mock UnidadeApi unidadeApi;
    @Mock DevolucaoApi devolucaoApi;
    @InjectMocks ManutencaoService service;

    @Test
    void iniciarAvulsa_numeroSerieInexistente_lancaRecursoNaoEncontrado() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarAvulsa("XXX", "defeito", 7L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(manutencoes, never()).saveAndFlush(any());
    }

    @Test
    void iniciarAvulsa_ok_colocaUnidadeEmManutencao_eSalvaSemVistoria() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        when(manutencoes.saveAndFlush(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.iniciarAvulsa("SN-1", "Motor falhando", 7L);

        verify(unidadeApi).iniciarManutencao(10L);
        assertThat(dto.vistoriaId()).isNull();
        assertThat(dto.usuarioTecnicoId()).isEqualTo(7L);
        assertThat(dto.status()).isEqualTo(StatusManutencao.EM_ANDAMENTO);
    }

    @Test
    void iniciarAvulsa_transicaoInvalidaDaUnidade_naoSalva() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        doThrow(new IllegalStateException("Transição de status inválida")).when(unidadeApi).iniciarManutencao(10L);

        assertThatThrownBy(() -> service.iniciarAvulsa("SN-1", "defeito", 7L))
                .isInstanceOf(IllegalStateException.class);
        verify(manutencoes, never()).saveAndFlush(any());
    }

    @Test
    void concluir_manutencaoInexistente_lancaRecursoNaoEncontrado() {
        when(manutencoes.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void concluir_jaConcluida_lancaIllegalState_eNaoLiberaNemRepassa() {
        Manutencao concluida = new Manutencao(10L, 99L, 7L, "defeito", INICIO);
        concluida.concluir(Instant.parse("2020-01-02T00:00:00Z"), BigDecimal.ONE);
        when(manutencoes.findById(5L)).thenReturn(Optional.of(concluida));

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
        verify(unidadeApi, never()).liberar(any());
        verifyNoInteractions(devolucaoApi);
    }

    @Test
    void concluir_avulsa_liberaUnidade_arredondaCusto_eNaoRepassaDanos() {
        Manutencao avulsa = new Manutencao(10L, null, 7L, "defeito", INICIO);
        when(manutencoes.findById(5L)).thenReturn(Optional.of(avulsa));
        when(manutencoes.saveAndFlush(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.concluir(5L, new BigDecimal("150.555"));

        verify(unidadeApi).liberar(10L);
        verifyNoInteractions(devolucaoApi);
        assertThat(dto.custoReparo()).isEqualByComparingTo("150.56");
        assertThat(dto.dataFim()).isNotNull();
        assertThat(dto.status()).isEqualTo(StatusManutencao.CONCLUIDA);
    }

    @Test
    void concluir_comVistoria_liberaUnidade_eRepassaDanosParaDevolucao() {
        Manutencao deVistoria = new Manutencao(10L, 99L, 7L, "Tela quebrada", INICIO);
        when(manutencoes.findById(5L)).thenReturn(Optional.of(deVistoria));
        when(manutencoes.saveAndFlush(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        service.concluir(5L, new BigDecimal("150.555"));

        verify(unidadeApi).liberar(10L);
        verify(devolucaoApi).registrarDanos(10L, new BigDecimal("150.56"));
    }

    @Test
    void historico_ok_retornaManutencoesDaUnidade() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        when(manutencoes.findByUnidadeIdOrderByDataInicioDesc(10L))
                .thenReturn(List.of(new Manutencao(10L, null, 7L, "defeito", INICIO)));

        assertThat(service.historico("SN-1")).hasSize(1);
    }
}