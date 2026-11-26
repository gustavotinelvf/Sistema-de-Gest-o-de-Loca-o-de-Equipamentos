package br.ufs.ziploc.locacao.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratoTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 10);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private Contrato contrato;
    private ItemContrato item;

    @BeforeEach
    void preparar() {
        Solicitacao solicitacao = new Solicitacao(1L, 1L, 2L, INICIO, FIM);
        solicitacao.adicionarItem(30L, 2, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(solicitacao, "id", 7L);
        ReflectionTestUtils.setField(solicitacao.getItens().get(0), "id", 8L);
        solicitacao.aprovar(3L);
        contrato = new Contrato(solicitacao, new BigDecimal("1000.00"), new BigDecimal("2.00"), "Devolver os equipamentos no prazo contratado.");
        contrato.adicionarItem(10L, 30L, new BigDecimal("100.00"));
        item = contrato.getItens().get(0);
        ReflectionTestUtils.setField(item, "id", 50L);
    }

    @Test
    void itemCopiaPeriodoEDiariaContratada() {
        assertThat(item.getDataInicioPrevista()).isEqualTo(INICIO);
        assertThat(item.getDataFimPrevista()).isEqualTo(FIM);
        assertThat(item.getValorDiariaContratada()).isEqualByComparingTo("100.00");
        assertThat(item.getLocadoraId()).isEqualTo(1L);
        assertThat(item.getSolicitacaoId()).isEqualTo(7L);
        assertThat(item.getItemSolicitacaoId()).isEqualTo(8L);
        assertThat(item.getModeloId()).isEqualTo(30L);
    }

    @Test
    void recusaDiariaInvalida() {
        assertThatThrownBy(() -> contrato.adicionarItem(11L, 30L, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contrato.adicionarItem(11L, 30L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retiradaRegistraDataEResponsavel() {
        contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getResponsavelRetiradaId()).isEqualTo(3L);
        assertThat(item.getDataInicioReal()).isEqualTo(INICIO);
    }

    @Test
    void recusaRetiradaSemDataOuResponsavel() {
        assertThatThrownBy(() -> contrato.ativar(null, 3L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.PLANEJADO);
    }

    @Test
    void recusaAlocacaoDuplicadaOuExcedente() {
        assertThatThrownBy(() -> contrato.adicionarItem(10L, 30L, new BigDecimal("100.00"))).isInstanceOf(IllegalArgumentException.class);
        contrato.adicionarItem(11L, 30L, new BigDecimal("100.00"));
        assertThatThrownBy(() -> contrato.adicionarItem(12L, 30L, new BigDecimal("100.00"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaModeloNaoSolicitado() {
        assertThatThrownBy(() -> contrato.adicionarItem(11L, 99L, new BigDecimal("100.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void finalizaApenasComTodasAsUnidadesDevolvidas() {
        contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        assertThatThrownBy(contrato::finalizar).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(item, "dataDevolucaoReal", FIM);
        assertThat(contrato.contarItensPendentes()).isZero();
        contrato.finalizar();
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.FINALIZADO);
    }

    @Test
    void cancelamentoPreservaHistoricoDosItens() {
        contrato.cancelar("Cliente desistiu");
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.CANCELADO);
        assertThat(contrato.getJustificativaCancelamento()).isEqualTo("Cliente desistiu");
        assertThat(contrato.getItens()).containsExactly(item);
    }
}