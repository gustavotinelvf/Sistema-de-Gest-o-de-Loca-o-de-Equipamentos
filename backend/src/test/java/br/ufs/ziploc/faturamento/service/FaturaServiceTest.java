package br.ufs.ziploc.faturamento.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import br.ufs.ziploc.faturamento.domain.Fatura;
import br.ufs.ziploc.faturamento.domain.StatusFatura;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.exception.FaturaJaExisteException;
import br.ufs.ziploc.faturamento.exception.FaturaNaoEncontradaException;
import br.ufs.ziploc.faturamento.repository.FaturaRepository;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private ContratoApi contratoApi;

    @InjectMocks
    private FaturaService faturaService;

    private Fatura criarFatura(StatusFatura status) {
        return Fatura.builder()
                .id(1L)
                .locadoraId(1L)
                .contratoId(10L)
                .status(status)
                .dataEmissao(LocalDate.of(2026, 1, 1))
                .dataVencimento(LocalDate.of(2026, 1, 8))
                .build();
    }

    private ContratoResumo criarContrato(LocalDate dataFim) {
        return new ContratoResumo(
                10L,
                1L,
                "ATIVO",
                dataFim.minusDays(10),
                dataFim,
                new BigDecimal("500.00"),
                new BigDecimal("10.00"),
                List.of()
        );
    }

    private void prepararGeracao(LocalDate dataFim) {
        when(faturaRepository.findByContratoId(10L))
                .thenReturn(Optional.empty());

        when(contratoApi.buscarContrato(10L))
                .thenReturn(Optional.of(criarContrato(dataFim)));

        when(faturaRepository.saveAndFlush(any(Fatura.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void devePersistirPagamentoAntesDeFinalizarContrato() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);

        when(faturaRepository.findByIdAndLocadoraId(1L, 1L))
                .thenReturn(Optional.of(fatura));

        FaturaDto resultado = faturaService.registrarPagamento(1L, 1L, 2L);

        assertEquals(StatusFatura.PAGA, resultado.status());
        assertEquals(2L, fatura.getResponsavelPagamentoId());
        assertNotNull(resultado.dataPagamento());

        InOrder ordem = inOrder(faturaRepository, contratoApi);

        ordem.verify(faturaRepository).findByIdAndLocadoraId(1L, 1L);
        ordem.verify(faturaRepository).saveAndFlush(fatura);
        ordem.verify(contratoApi).finalizarContrato(10L);
    }

    @Test
    void deveRejeitarPagamentoDeFaturaInexistente() {
        when(faturaRepository.findByIdAndLocadoraId(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(FaturaNaoEncontradaException.class,
                () -> faturaService.registrarPagamento(99L, 1L, 2L));

        verify(faturaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contratoApi);
    }

    @Test
    void deveImpedirPagamentoDuplicadoSemFinalizarContrato() {
        Fatura fatura = criarFatura(StatusFatura.PAGA);

        when(faturaRepository.findByIdAndLocadoraId(1L, 1L))
                .thenReturn(Optional.of(fatura));

        assertThrows(IllegalStateException.class,
                () -> faturaService.registrarPagamento(1L, 1L, 2L));

        verify(faturaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contratoApi);
    }

    @Test
    void naoDeveFinalizarContratoSePersistenciaFalhar() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);

        when(faturaRepository.findByIdAndLocadoraId(1L, 1L))
                .thenReturn(Optional.of(fatura));

        when(faturaRepository.saveAndFlush(fatura))
                .thenThrow(new IllegalStateException("Falha na persistência"));

        assertThrows(IllegalStateException.class,
                () -> faturaService.registrarPagamento(1L, 1L, 2L));

        verifyNoInteractions(contratoApi);
    }

    @Test
    void deveGerarFaturaComValoresCorretos() {
        prepararGeracao(LocalDate.now(ZoneId.of("America/Sao_Paulo")));

        FaturaDto resultado = faturaService.gerarFatura(
                10L, 1L, 2L, new BigDecimal("50.00"));

        ArgumentCaptor<Fatura> captor = ArgumentCaptor.forClass(Fatura.class);

        verify(faturaRepository).saveAndFlush(captor.capture());

        Fatura fatura = captor.getValue();

        assertEquals(10L, fatura.getContratoId());
        assertEquals(1L, fatura.getLocadoraId());
        assertEquals(2L, fatura.getResponsavelEmissaoId());
        assertEquals(new BigDecimal("500.00"), fatura.getValorLocacao());
        assertEquals(new BigDecimal("50.00"), fatura.getValorMulta());
        assertEquals(StatusFatura.PENDENTE, resultado.status());
        assertNull(fatura.getDataPagamento());
        assertNull(fatura.getResponsavelPagamentoId());

        verify(contratoApi, never()).finalizarContrato(anyLong());
    }

    @Test
    void deveCalcularVencimentoAPartirDaEmissao() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        prepararGeracao(hoje.plusDays(2));

        faturaService.gerarFatura(10L, 1L, 2L, BigDecimal.ZERO);

        ArgumentCaptor<Fatura> captor = ArgumentCaptor.forClass(Fatura.class);

        verify(faturaRepository).saveAndFlush(captor.capture());

        Fatura fatura = captor.getValue();

        assertFalse(fatura.getDataEmissao().isBefore(hoje));
        assertFalse(fatura.getDataEmissao().isAfter(
                LocalDate.now(ZoneId.of("America/Sao_Paulo"))));
        assertEquals(fatura.getDataEmissao().plusDays(7),
                fatura.getDataVencimento());
    }

    @Test
    void deveGerarFaturaMesmoComDevolucaoAtrasada() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        prepararGeracao(hoje.minusDays(20));

        FaturaDto resultado = faturaService.gerarFatura(
                10L, 1L, 2L, new BigDecimal("100.00"));

        ArgumentCaptor<Fatura> captor = ArgumentCaptor.forClass(Fatura.class);

        verify(faturaRepository).saveAndFlush(captor.capture());

        Fatura fatura = captor.getValue();

        assertEquals(StatusFatura.PENDENTE, resultado.status());
        assertTrue(fatura.getDataVencimento().isAfter(fatura.getDataEmissao()));
        assertEquals(fatura.getDataEmissao().plusDays(7),
                fatura.getDataVencimento());
    }

    @Test
    void deveImpedirGeracaoDeFaturaDuplicada() {
        when(faturaRepository.findByContratoId(10L))
                .thenReturn(Optional.of(criarFatura(StatusFatura.PENDENTE)));

        assertThrows(FaturaJaExisteException.class,
                () -> faturaService.gerarFatura(
                        10L, 1L, 2L, BigDecimal.ZERO));

        verify(faturaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contratoApi);
    }

    @Test
    void deveRejeitarGeracaoParaContratoInexistente() {
        when(faturaRepository.findByContratoId(99L))
                .thenReturn(Optional.empty());

        when(contratoApi.buscarContrato(99L))
                .thenReturn(Optional.empty());

        assertThrows(ContratoNaoEncontradoException.class,
                () -> faturaService.gerarFatura(
                        99L, 1L, 2L, BigDecimal.ZERO));

        verify(faturaRepository, never()).saveAndFlush(any());
    }
}