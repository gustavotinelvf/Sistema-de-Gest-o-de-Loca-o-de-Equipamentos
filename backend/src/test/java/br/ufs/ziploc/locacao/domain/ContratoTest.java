package br.ufs.ziploc.locacao.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratoTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 10);
    private Contrato contrato;
    private ItemContrato item;

    @BeforeEach
    void preparar() {
        Solicitacao solicitacao = new Solicitacao(1L, 2L, INICIO, FIM);
        solicitacao.aprovar(3L);
        contrato = new Contrato(solicitacao, new BigDecimal("1000.00"), new BigDecimal("2.00"));
        contrato.adicionarItem(10L, new BigDecimal("100.00"));
        item = contrato.getItens().get(0);
        ReflectionTestUtils.setField(item, "id", 50L);
    }

    @Test
    void itemCopiaPeriodoEDiariaContratada() {
        assertThat(item.getDataInicioPrevista()).isEqualTo(INICIO);
        assertThat(item.getDataFimPrevista()).isEqualTo(FIM);
        assertThat(item.getValorDiariaContratada()).isEqualByComparingTo("100.00");
    }

    @Test
    void recusaDiariaInvalida() {
        assertThatThrownBy(() -> contrato.adicionarItem(11L, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contrato.adicionarItem(11L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registraDevolucaoEmContratoAtivo() {
        contrato.ativar();
        item.registrarInicioReal(INICIO);
        contrato.registrarDevolucao(50L, FIM.plusDays(2));
        assertThat(item.getDataDevolucaoReal()).isEqualTo(FIM.plusDays(2));
        assertThat(contrato.contarItensPendentes()).isZero();
    }

    @Test
    void recusaDevolucaoForaDeContratoAtivo() {
        assertThatThrownBy(() -> contrato.registrarDevolucao(50L, FIM)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaDevolucaoSemRetiradaOuRepetida() {
        contrato.ativar();
        assertThatThrownBy(() -> contrato.registrarDevolucao(50L, FIM)).isInstanceOf(IllegalStateException.class);
        item.registrarInicioReal(INICIO);
        assertThatThrownBy(() -> contrato.registrarDevolucao(50L, INICIO.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        contrato.registrarDevolucao(50L, FIM);
        assertThatThrownBy(() -> contrato.registrarDevolucao(50L, FIM)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaItemDeOutroContrato() {
        contrato.ativar();
        assertThatThrownBy(() -> contrato.registrarDevolucao(99L, FIM)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void finalizaApenasComTodasAsUnidadesDevolvidas() {
        contrato.ativar();
        item.registrarInicioReal(INICIO);
        assertThatThrownBy(contrato::finalizar).isInstanceOf(IllegalStateException.class);
        contrato.registrarDevolucao(50L, FIM);
        contrato.finalizar();
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.FINALIZADO);
    }
}
