package br.ufs.ziploc.locacao.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void calculaValorNoPeriodoMinimoPermitido() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(1));

        solicitacao.adicionarItem(30L, 2, new BigDecimal("50.00"));

        assertThat(solicitacao.getQuantidadeDias()).isEqualTo(2);
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("200.00");
    }

    @Test
    void calculaPeriodoNaViradaDoMes() {
        Solicitacao solicitacao = new Solicitacao(
                1L, 10L, 20L,
                LocalDate.of(2027, 1, 30),
                LocalDate.of(2027, 2, 2));

        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThat(solicitacao.getQuantidadeDias()).isEqualTo(4);
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("400.00");
    }

    @Test
    void calculaPeriodoNaViradaDoAno() {
        Solicitacao solicitacao = new Solicitacao(
                1L, 10L, 20L,
                LocalDate.of(2027, 12, 30),
                LocalDate.of(2028, 1, 2));

        solicitacao.adicionarItem(30L, 2, new BigDecimal("100.00"));

        assertThat(solicitacao.getQuantidadeDias()).isEqualTo(4);
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("800.00");
    }

    @Test
    void consideraDiaVinteENoveDeFevereiroEmAnoBissexto() {
        Solicitacao solicitacao = new Solicitacao(
                1L, 10L, 20L,
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 3, 1));

        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThat(solicitacao.getQuantidadeDias()).isEqualTo(3);
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
    }

    @Test
    void calculaPeriodoEmFevereiroDeAnoNaoBissexto() {
        Solicitacao solicitacao = new Solicitacao(
                1L, 10L, 20L,
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 1));

        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThat(solicitacao.getQuantidadeDias()).isEqualTo(2);
        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("200.00");
    }

    @Test
    void preservaPrecisaoMonetariaComCentavos() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));

        solicitacao.adicionarItem(30L, 2, new BigDecimal("99.99"));
        solicitacao.adicionarItem(31L, 3, new BigDecimal("10.01"));

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("690.03");
        assertThat(solicitacao.getItens()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeInvalidaSemAlterarEstimativa(int quantidade) {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, quantidade, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-10.00"})
    void rejeitaDiariaInvalidaSemAlterarEstimativa(String diaria) {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, 1, new BigDecimal(diaria)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }

    @Test
    void rejeitaDiariaNulaSemAlterarEstimativa() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }

    @Test
    void rejeitaPeriodoComInicioEFimNoMesmoDia() {
        assertThatThrownBy(() ->
                new Solicitacao(1L, 10L, 20L, INICIO, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaPeriodoComFimAnteriorAoInicio() {
        assertThatThrownBy(() ->
                new Solicitacao(1L, 10L, 20L, INICIO, INICIO.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void impedeInclusaoDeItensEmSolicitacaoAprovada() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));
        solicitacao.aprovar(40L);

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, 1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }

    @Test
    void impedeInclusaoDeItensEmSolicitacaoRejeitada() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));
        solicitacao.rejeitar(40L, "Solicitação rejeitada.");

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, 1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }

    @Test
    void impedeInclusaoDeItensEmSolicitacaoCancelada() {
        Solicitacao solicitacao = new Solicitacao(1L, 10L, 20L, INICIO, INICIO.plusDays(2));
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));
        solicitacao.cancelar("Cliente desistiu.");

        assertThatThrownBy(() ->
                solicitacao.adicionarItem(31L, 1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(solicitacao.getValorEstimado()).isEqualByComparingTo("300.00");
        assertThat(solicitacao.getItens()).hasSize(1);
    }
}