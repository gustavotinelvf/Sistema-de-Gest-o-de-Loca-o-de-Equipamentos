package br.ufs.ziploc.faturamento.domain;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FaturaTest {

    private Fatura criarFatura(StatusFatura status) {
        return Fatura.builder()
                .status(status)
                .dataEmissao(LocalDate.of(2026, 11, 20))
                .dataVencimento(LocalDate.of(2026, 11, 27))
                .build();
    }

    @Test
    void deveRegistrarPagamentoDeFaturaPendente() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);
        LocalDate data = LocalDate.of(2026, 11, 26);

        fatura.marcarPago(data, 1L);

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        assertEquals(data, fatura.getDataPagamento());
        assertEquals(1L, fatura.getResponsavelPagamentoId());
    }

    @Test
    void devePermitirPagamentoDeFaturaVencida() {
        Fatura fatura = criarFatura(StatusFatura.VENCIDA);

        fatura.marcarPago(LocalDate.of(2026, 11, 28), 1L);

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
    }

    @Test
    void deveImpedirPagamentoDuplicado() {
        Fatura fatura = criarFatura(StatusFatura.PAGA);

        assertThrows(IllegalStateException.class,
                () -> fatura.marcarPago(LocalDate.of(2026, 11, 26), 1L));
    }

    @Test
    void deveImpedirPagamentoDeFaturaCancelada() {
        Fatura fatura = criarFatura(StatusFatura.CANCELADA);

        assertThrows(IllegalStateException.class,
                () -> fatura.marcarPago(LocalDate.of(2026, 11, 26), 1L));
    }

    @Test
    void deveRejeitarPagamentoSemResponsavel() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);

        assertThrows(IllegalArgumentException.class,
                () -> fatura.marcarPago(LocalDate.of(2026, 11, 26), null));
    }

    @Test
    void deveRejeitarPagamentoSemData() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);

        assertThrows(IllegalArgumentException.class,
                () -> fatura.marcarPago(null, 1L));
    }

    @Test
    void deveRejeitarPagamentoAnteriorAEmissao() {
        Fatura fatura = criarFatura(StatusFatura.PENDENTE);

        assertThrows(IllegalArgumentException.class,
                () -> fatura.marcarPago(LocalDate.of(2026, 11, 19), 1L));
    }
}