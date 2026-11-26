package br.ufs.ziploc.faturamento.service;

import java.time.LocalDate;
import java.util.Optional;

import br.ufs.ziploc.faturamento.domain.Fatura;
import br.ufs.ziploc.faturamento.domain.StatusFatura;
import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.exception.FaturaNaoEncontradaException;
import br.ufs.ziploc.faturamento.repository.FaturaRepository;
import br.ufs.ziploc.locacao.api.ContratoApi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
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
}