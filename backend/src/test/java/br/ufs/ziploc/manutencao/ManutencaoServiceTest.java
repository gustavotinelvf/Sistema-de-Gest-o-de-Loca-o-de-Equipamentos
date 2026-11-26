package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;
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

    private static final Long LOCADORA_ID = 1L;
    private static final Instant INICIO = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant PREVISTA_PASSADA = Instant.parse("2020-01-02T00:00:00Z");

    @Mock ManutencaoRepository manutencoes;
    @Mock UnidadeApi unidadeApi;
    @Mock DevolucaoApi devolucaoApi;
    @InjectMocks ManutencaoService service;

    private static Manutencao aberta(TipoManutencao tipo, Instant dataPrevista) {
        return new Manutencao(LOCADORA_ID, 10L, 7L, tipo, "defeito", dataPrevista, INICIO);
    }

    private void salvarRetornandoArgumento() {
        when(manutencoes.saveAndFlush(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void iniciarAvulsa_numeroSerieInexistente_lancaRecursoNaoEncontrado() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarAvulsa(LOCADORA_ID, "XXX", TipoManutencao.PREVENTIVA,
                "revisão", null, 7L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(manutencoes, never()).saveAndFlush(any());
    }

    @Test
    void iniciarAvulsa_ok_colocaUnidadeEmManutencao_eSalvaEmAberto() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        salvarRetornandoArgumento();

        var dto = service.iniciarAvulsa(LOCADORA_ID, "SN-1", TipoManutencao.PREVENTIVA, "Revisão", null, 7L);

        verify(unidadeApi).iniciarManutencao(10L);
        assertThat(dto.tipo()).isEqualTo(TipoManutencao.PREVENTIVA);
        assertThat(dto.tecnicoId()).isEqualTo(7L);
        assertThat(dto.dataConclusao()).isNull();
    }

    @Test
    void iniciarAvulsa_transicaoInvalidaDaUnidade_naoSalva() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        doThrow(new IllegalStateException("Transição de status inválida")).when(unidadeApi).iniciarManutencao(10L);

        assertThatThrownBy(() -> service.iniciarAvulsa(LOCADORA_ID, "SN-1", TipoManutencao.CORRETIVA,
                "defeito", null, 7L))
                .isInstanceOf(IllegalStateException.class);
        verify(manutencoes, never()).saveAndFlush(any());
    }

    @Test
    void concluir_manutencaoInexistente_lancaRecursoNaoEncontrado() {
        when(manutencoes.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void concluir_jaConcluida_lancaIllegalState_eNaoLiberaNemRepassa() {
        Manutencao concluida = aberta(TipoManutencao.CORRETIVA, null);
        concluida.concluir(Instant.parse("2020-01-03T00:00:00Z"), BigDecimal.ONE, null);
        when(manutencoes.findById(5L)).thenReturn(Optional.of(concluida));

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN, null))
                .isInstanceOf(IllegalStateException.class);
        verify(unidadeApi, never()).liberar(any());
        verifyNoInteractions(devolucaoApi);
    }

    @Test
    void concluir_preventiva_liberaUnidade_arredondaCusto_eNaoRepassaDanos() {
        when(manutencoes.findById(5L)).thenReturn(Optional.of(aberta(TipoManutencao.PREVENTIVA, null)));
        salvarRetornandoArgumento();

        var dto = service.concluir(5L, new BigDecimal("150.555"), null);

        verify(unidadeApi).liberar(10L);
        verifyNoInteractions(devolucaoApi);
        assertThat(dto.custoGerado()).isEqualByComparingTo("150.56");
        assertThat(dto.dataConclusao()).isNotNull();
    }

    @Test
    void concluir_corretivaComCusto_liberaUnidade_eRepassaDanosParaDevolucao() {
        when(manutencoes.findById(5L)).thenReturn(Optional.of(aberta(TipoManutencao.CORRETIVA, null)));
        salvarRetornandoArgumento();

        service.concluir(5L, new BigDecimal("150.555"), null);

        verify(unidadeApi).liberar(10L);
        verify(devolucaoApi).registrarDanos(10L, new BigDecimal("150.56"));
    }

    @Test
    void concluir_corretivaSemCusto_liberaUnidade_masNaoRepassa() {
        when(manutencoes.findById(5L)).thenReturn(Optional.of(aberta(TipoManutencao.CORRETIVA, null)));
        salvarRetornandoArgumento();

        service.concluir(5L, BigDecimal.ZERO, null);

        verify(unidadeApi).liberar(10L);
        verifyNoInteractions(devolucaoApi);
    }

    @Test
    void concluir_aposDataPrevistaSemJustificativa_RD023_lancaIllegalArgument() {
        when(manutencoes.findById(5L)).thenReturn(Optional.of(aberta(TipoManutencao.CORRETIVA, PREVISTA_PASSADA)));

        assertThatThrownBy(() -> service.concluir(5L, BigDecimal.TEN, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(unidadeApi, never()).liberar(any());
    }

    @Test
    void concluir_aposDataPrevistaComJustificativa_concluiNormalmente() {
        when(manutencoes.findById(5L)).thenReturn(Optional.of(aberta(TipoManutencao.PREVENTIVA, PREVISTA_PASSADA)));
        salvarRetornandoArgumento();

        var dto = service.concluir(5L, BigDecimal.TEN, "Peça importada atrasou");

        verify(unidadeApi).liberar(10L);
        assertThat(dto.justificativaAtraso()).isEqualTo("Peça importada atrasou");
    }

    @Test
    void historico_ok_retornaManutencoesDaUnidade() {
        when(unidadeApi.buscarUnidadeIdPorNumeroSerie("SN-1")).thenReturn(Optional.of(10L));
        when(manutencoes.findByUnidadeIdOrderByDataInicioDesc(10L))
                .thenReturn(List.of(aberta(TipoManutencao.PREVENTIVA, null)));

        assertThat(service.historico("SN-1")).hasSize(1);
    }
}