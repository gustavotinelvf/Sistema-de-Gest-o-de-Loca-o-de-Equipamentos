package br.ufs.ziploc.locacao.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitacaoTest {
    private static final LocalDate INICIO = LocalDate.of(2027, 12, 1);

    @Test
    void contaODiaDeInicioEODeFim() {
        assertThat(new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(1)).getQuantidadeDias()).isEqualTo(2);
        assertThat(new Solicitacao(1L, 10L, 20L, INICIO, LocalDate.of(2027, 12, 10)).getQuantidadeDias()).isEqualTo(10);
    }

    @Test
    void valorEstimadoMultiplicaDiariaQuantidadeEDias_RF046() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, LocalDate.of(2027, 12, 10));
        solicitacao.adicionarItem(30L, 2, new BigDecimal("100.00"));
        solicitacao.adicionarItem(31L, 1, new BigDecimal("50.00"));
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("2500.00");
    }

    @Test
    void solicitacaoSemItensTemValorEstimadoZero() {
        assertThat(new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(3)).getValorEstimado()).isEqualByComparingTo("0");
    }
}
