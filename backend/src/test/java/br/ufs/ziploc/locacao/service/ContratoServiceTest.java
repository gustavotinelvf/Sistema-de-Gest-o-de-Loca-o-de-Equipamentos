package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import br.ufs.ziploc.locacao.dto.ContratoListagemResponse;
import br.ufs.ziploc.locacao.dto.ContratoResponse;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 11);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock ContratoRepository contratos;
    @Mock UnidadeApi unidadeApi;
    @Mock EntityManager entityManager;
    @Mock LocadoraApi locadoraApi;
    @InjectMocks ContratoService service;

    private void prepararLocadora(Long id) {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);
        when(parametros.id()).thenReturn(id);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
    }

    private Contrato contratoPlanejado(Long... unidadeIds) {
        Solicitacao s = new Solicitacao(1L, 1L, 2L, INICIO, FIM);
        ReflectionTestUtils.setField(s, "id", 7L);
        if (unidadeIds.length > 0) {
            s.adicionarItem(30L, unidadeIds.length, new BigDecimal("100.00"));
            ReflectionTestUtils.setField(s.getItens().get(0), "id", 8L);
        }
        s.aprovar(3L);
        Contrato c = new Contrato(s, new BigDecimal("1000.00"), new BigDecimal("10.00"), "Devolver os equipamentos no prazo contratado.");
        for (Long id : unidadeIds) c.adicionarItem(id, 30L, new BigDecimal("100.00"));
        return c;
    }

    private UnidadeInfo unidade(Long id, String status, String diaria) {
        return new UnidadeInfo(id, 30L, "SN-" + id, status, true, new BigDecimal(diaria));
    }

    // ---------- retirada ----------

    @Test
    void retiradaAtivaContratoRegistraInicioRealEResponsavel() {
        Contrato c = contratoPlanejado(10L, 11L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));
        when(unidadeApi.buscarUnidade(10L)).thenReturn(Optional.of(unidade(10L, "DISPONIVEL", "100.00")));
        when(unidadeApi.buscarUnidade(11L)).thenReturn(Optional.of(unidade(11L, "DISPONIVEL", "50.00")));
        when(contratos.saveAndFlush(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));

        ContratoResponse r = service.retirar(1L, INICIO, 3L);

        assertThat(r.status()).isEqualTo(StatusContrato.ATIVO);
        assertThat(r.itens()).allSatisfy(i -> assertThat(i.dataInicioReal()).isEqualTo(INICIO));
        assertThat(c.getResponsavelRetiradaId()).isEqualTo(3L);
        verify(contratos).saveAndFlush(c);
        verify(unidadeApi, never()).locar(any());
    }

    @Test
    void retiradaRecusadaSeAlgumaUnidadeNaoEstaDisponivel() {
        Contrato c = contratoPlanejado(10L, 11L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));
        when(unidadeApi.buscarUnidade(10L)).thenReturn(Optional.of(unidade(10L, "DISPONIVEL", "100.00")));
        when(unidadeApi.buscarUnidade(11L)).thenReturn(Optional.of(unidade(11L, "EM_MANUTENCAO", "50.00")));

        assertThatThrownBy(() -> service.retirar(1L, INICIO, 3L)).isInstanceOf(IllegalStateException.class);

        assertThat(c.getStatus()).isEqualTo(StatusContrato.PLANEJADO);
        verify(unidadeApi, never()).locar(any());
        verify(contratos, never()).saveAndFlush(any());
    }

    @Test
    void retiradaRecusadaParaContratoSemItensOuJaAtivo() {
        Contrato semItens = contratoPlanejado();
        Contrato ativo = contratoPlanejado(10L);
        ativo.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(semItens));
        when(contratos.findByIdForUpdate(2L)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> service.retirar(1L, INICIO, 3L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.retirar(2L, INICIO, 3L)).isInstanceOf(IllegalStateException.class);
    }

    // ---------- cancelamento ----------

    @Test
    void cancelamentoRegistraJustificativaEPreservaHistorico() {
        Contrato c = contratoPlanejado(10L, 11L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));
        when(contratos.saveAndFlush(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));

        ContratoResponse r = service.cancelar(1L, "Cliente desistiu");

        assertThat(r.status()).isEqualTo(StatusContrato.CANCELADO);
        assertThat(r.justificativaCancelamento()).isEqualTo("Cliente desistiu");
        assertThat(r.itens()).hasSize(2);
        verify(contratos).saveAndFlush(c);
    }

    @Test
    void cancelamentoExigeJustificativaENaoPermiteContratoAtivo() {
        Contrato planejado = contratoPlanejado(10L);
        Contrato ativo = contratoPlanejado(10L);
        ativo.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(planejado));
        when(contratos.findByIdForUpdate(2L)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> service.cancelar(1L, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cancelar(2L, "motivo")).isInstanceOf(IllegalStateException.class);
    }

    // ---------- prorrogação ----------

    private Contrato contratoAtivo(Long... unidadeIds) {
        Contrato c = contratoPlanejado(unidadeIds);
        c.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        return c;
    }

    @Test
    void prorrogacaoSomaDiariasContratadasDasUnidadesVezesDiasAdicionais() {
        Contrato c = contratoAtivo(10L, 11L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));

        service.prorrogar(1L, FIM.plusDays(3));

        assertThat(c.getDataFimPrevista()).isEqualTo(FIM.plusDays(3));
        assertThat(c.getValorTotal()).isEqualByComparingTo("1600.00");
        verify(contratos).saveAndFlush(c);
        verify(entityManager).refresh(c);
        verifyNoInteractions(unidadeApi);
    }

    @Test
    void prorrogacaoRecusadaParaContratoPlanejadoOuDataNaoPosterior() {
        Contrato planejado = contratoPlanejado(10L);
        Contrato ativo = contratoAtivo(10L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(planejado));
        when(contratos.findByIdForUpdate(2L)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> service.prorrogar(1L, FIM.plusDays(1))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.prorrogar(2L, FIM)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prorrogacaoRecusadaQuandoHaUnidadeJaDevolvida() {
        Contrato c = contratoAtivo(10L, 11L);
        ReflectionTestUtils.setField(c.getItens().get(0), "dataDevolucaoReal", FIM.minusDays(2));
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.prorrogar(1L, FIM.plusDays(2))).isInstanceOf(IllegalStateException.class);
        verify(contratos, never()).saveAndFlush(any());
    }

    @Test
    void prorrogacaoTraduzOverbookingDoBancoParaConflito() {
        Contrato c = contratoAtivo(10L);
        when(contratos.findByIdForUpdate(1L)).thenReturn(Optional.of(c));
        when(contratos.saveAndFlush(any(Contrato.class)))
                .thenThrow(new DataIntegrityViolationException("excl", new SQLException("conflicting key", "23P01")));

        assertThatThrownBy(() -> service.prorrogar(1L, FIM.plusDays(2))).isInstanceOf(ConflitoException.class);
        verify(entityManager, never()).refresh(any());
    }

    // ---------- listagem por status ----------

    @Test
    void listaContratosDoStatusPedido() {
        prepararLocadora(1L);
        Contrato c = contratoPlanejado(10L);
        ReflectionTestUtils.setField(c, "id", 15L);

        when(contratos.findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(
                1L, StatusContrato.PLANEJADO)).thenReturn(List.of(c));

        List<ContratoListagemResponse> r = service.listar(StatusContrato.PLANEJADO);

        assertThat(r).singleElement().satisfies(linha -> {
            assertThat(linha.id()).isEqualTo(15L);
            assertThat(linha.clienteId()).isEqualTo(1L);
            assertThat(linha.solicitacaoId()).isEqualTo(7L);
            assertThat(linha.status()).isEqualTo(StatusContrato.PLANEJADO);
            assertThat(linha.dataInicioPrevista()).isEqualTo(INICIO);
            assertThat(linha.dataFimPrevista()).isEqualTo(FIM);
            assertThat(linha.valorTotal()).isEqualByComparingTo("1000.00");
        });

        verify(contratos).findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(
                1L, StatusContrato.PLANEJADO);

        verify(contratos, never()).findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(any());
    }

    @Test
    void semStatusListaTodosOsContratos() {
        prepararLocadora(1L);
        Contrato planejado = contratoPlanejado(10L);
        Contrato ativo = contratoAtivo(11L);

        when(contratos.findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(1L))
                .thenReturn(List.of(planejado, ativo));

        assertThat(service.listar(null))
                .extracting(ContratoListagemResponse::status)
                .containsExactly(StatusContrato.PLANEJADO, StatusContrato.ATIVO);

        verify(contratos, never())
                .findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(any(), any());
    }

    @ParameterizedTest
    @EnumSource(StatusContrato.class)
    void consultaContratosParaCadaStatus(StatusContrato status) {
        prepararLocadora(1L);

        when(contratos.findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(1L, status))
                .thenReturn(List.of());

        assertThat(service.listar(status)).isEmpty();

        verify(contratos).findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(1L, status);
        verify(contratos, never()).findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(any());
    }

    @Test
    void retornaListaVaziaSemFiltro() {
        prepararLocadora(1L);

        when(contratos.findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(1L))
                .thenReturn(List.of());

        assertThat(service.listar(null)).isEmpty();

        verify(contratos).findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void impedeListagemComLocadoraInvalida(Long locadoraId) {
        prepararLocadora(locadoraId);

        Class<? extends RuntimeException> excecao = locadoraId == null
                ? NullPointerException.class
                : IllegalStateException.class;

        assertThatThrownBy(() -> service.listar(null))
                .isInstanceOf(excecao);

        verifyNoInteractions(contratos);
    }

    @Test
    void utilizaLocadoraFornecidaPelaApiSemFiltro() {
        prepararLocadora(42L);

        when(contratos.findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(42L))
                .thenReturn(List.of());

        assertThat(service.listar(null)).isEmpty();

        verify(contratos).findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(42L);
    }

    @Test
    void utilizaLocadoraFornecidaPelaApiComFiltro() {
        prepararLocadora(42L);

        when(contratos.findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(
                42L, StatusContrato.ATIVO)).thenReturn(List.of());

        assertThat(service.listar(StatusContrato.ATIVO)).isEmpty();

        verify(contratos).findByLocadoraIdAndStatusOrderByDataInicioPrevistaAscIdAsc(
                42L, StatusContrato.ATIVO);
    }

    @Test
    void montaListagemSemConsultarItensDoContrato() {
        prepararLocadora(1L);

        Contrato c = mock(Contrato.class);
        when(c.getId()).thenReturn(15L);

        when(contratos.findByLocadoraIdOrderByDataInicioPrevistaAscIdAsc(1L))
                .thenReturn(List.of(c));

        assertThat(service.listar(null))
                .extracting(ContratoListagemResponse::id)
                .containsExactly(15L);

        verify(c, never()).getItens();
        verifyNoInteractions(unidadeApi);
    }
}