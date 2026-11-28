package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.cliente.api.ClienteApi;
import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.ModeloInfo;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.RegistrarSolicitacaoRequest;
import br.ufs.ziploc.locacao.dto.SolicitacaoResponse;
import br.ufs.ziploc.locacao.dto.SolicitacaoResumo;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Mock SolicitacaoRepository solicitacoes;
    @Mock UnidadeApi unidadeApi;
    @Mock LocadoraApi locadoraApi;
    @Mock ClienteApi clienteApi;
    @InjectMocks SolicitacaoService service;

    private void prepararLocadora() {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);
        when(parametros.id()).thenReturn(1L);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
    }

    private void prepararCliente(boolean ativo, boolean adimplente) {
        prepararCliente(1L, ativo,
                adimplente ? StatusAdimplencia.ADIMPLENTE : StatusAdimplencia.INADIMPLENTE);
    }

    private void prepararCliente(Long locadoraId, boolean ativo, StatusAdimplencia status) {
        when(clienteApi.consultarResumo(10L)).thenReturn(
                new ClienteResumoDTO(10L, locadoraId, "Construtora", "12345678000199", "Ana",
                        "11122233344", "ana@teste.com", "79999999999", status,
                        ativo, "49100000", "Rua B", "20", null, "Centro", "São Cristóvão", "SE"));
    }

    private ModeloInfo modelo(boolean ativo) {
        return new ModeloInfo(1L, "Betoneira", "Construção", new BigDecimal("100.00"), 1, 5, ativo);
    }

    private RegistrarSolicitacaoRequest request(LocalDate inicio, RegistrarSolicitacaoRequest.Item... itens) {
        return new RegistrarSolicitacaoRequest(10L, inicio, inicio.plusDays(5), List.of(itens));
    }

    @Test
    void registraSolicitacaoPendenteComItens() {
        prepararCliente(true, true);
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(true)));
        when(solicitacoes.saveAndFlush(any(Solicitacao.class))).thenAnswer(i -> i.getArgument(0));

        SolicitacaoResponse r = service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 3)), 20L);

        assertThat(r.status()).isEqualTo(StatusSolicitacao.PENDENTE);
        assertThat(r.itens()).hasSize(1);
        assertThat(r.itens().get(0).quantidade()).isEqualTo(3);
        assertThat(r.clienteId()).isEqualTo(10L);
        assertThat(r.valorEstimado()).isEqualByComparingTo("1800.00");

        verify(clienteApi).consultarResumo(10L);
        verify(clienteApi, never()).verificarAdimplencia(any());
        verify(solicitacoes).saveAndFlush(any(Solicitacao.class));
    }

    @Test
    void rejeitaModeloInativo() {
        prepararCliente(true, true);
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(false)));

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(solicitacoes);
    }

    @Test
    void rejeitaModeloInexistente() {
        prepararCliente(true, true);
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(solicitacoes);
    }

    @Test
    void rejeitaModeloRepetidoNosItens() {
        prepararCliente(true, true);
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(true)));

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1),
                new RegistrarSolicitacaoRequest.Item(1L, 1), new RegistrarSolicitacaoRequest.Item(1L, 2)), 20L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(solicitacoes);
    }

    @Test
    void rejeitaDataDeInicioNoPassado() {
        assertThatThrownBy(() -> service.registrar(request(HOJE.minusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(locadoraApi, clienteApi, unidadeApi, solicitacoes);
    }

    @Test
    void cancelaSolicitacaoPendenteComJustificativa() {
        Solicitacao s = new Solicitacao(1L, 10L, 20L, HOJE.plusDays(1), HOJE.plusDays(3));
        when(solicitacoes.findComTravaById(5L)).thenReturn(Optional.of(s));

        SolicitacaoResponse r = service.cancelar(5L, "  Cliente desistiu ");

        assertThat(r.status()).isEqualTo(StatusSolicitacao.CANCELADA);
        assertThat(r.justificativa()).isEqualTo("Cliente desistiu");
    }

    @Test
    void naoCancelaSemJustificativaNemSolicitacaoJaAnalisada_RD012() {
        Solicitacao pendente = new Solicitacao(1L, 10L, 20L, HOJE.plusDays(1), HOJE.plusDays(3));
        Solicitacao aprovada = new Solicitacao(1L, 10L, 20L, HOJE.plusDays(1), HOJE.plusDays(3));
        aprovada.aprovar(30L);
        when(solicitacoes.findComTravaById(1L)).thenReturn(Optional.of(pendente));
        when(solicitacoes.findComTravaById(2L)).thenReturn(Optional.of(aprovada));

        assertThatThrownBy(() -> service.cancelar(1L, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cancelar(2L, "motivo")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listaFilaDaLocadoraFiltradaPorStatus_RF010() {
        prepararLocadora();
        Solicitacao pendente = new Solicitacao(1L, 10L, 20L, HOJE.plusDays(1), HOJE.plusDays(3));
        pendente.adicionarItem(1L, 2, new BigDecimal("100.00"));
        when(solicitacoes.findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(1L, StatusSolicitacao.PENDENTE)).thenReturn(List.of(pendente));

        List<SolicitacaoResumo> fila = service.listar(StatusSolicitacao.PENDENTE);

        assertThat(fila).singleElement().satisfies(r -> {
            assertThat(r.clienteId()).isEqualTo(10L);
            assertThat(r.status()).isEqualTo(StatusSolicitacao.PENDENTE);
            assertThat(r.valorEstimado()).isEqualByComparingTo("600.00");
        });

        verify(solicitacoes, never()).findByLocadoraIdOrderByDataCriacaoAscIdAsc(any());
    }

    @Test
    void listaTodasAsSolicitacoesSemFiltro() {
        prepararLocadora();
        Solicitacao aprovada = new Solicitacao(1L, 10L, 20L, HOJE.plusDays(1), HOJE.plusDays(3));
        aprovada.aprovar(30L);
        Solicitacao pendente = new Solicitacao(1L, 11L, 20L, HOJE.plusDays(2), HOJE.plusDays(4));
        when(solicitacoes.findByLocadoraIdOrderByDataCriacaoAscIdAsc(1L)).thenReturn(List.of(aprovada, pendente));

        assertThat(service.listar(null)).extracting(SolicitacaoResumo::status)
                .containsExactly(StatusSolicitacao.APROVADA, StatusSolicitacao.PENDENTE);
    }

    @ParameterizedTest
    @EnumSource(StatusSolicitacao.class)
    void consultaFilaParaCadaStatus(StatusSolicitacao status) {
        prepararLocadora();

        when(solicitacoes.findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(1L, status))
                .thenReturn(List.of());

        assertThat(service.listar(status)).isEmpty();

        verify(solicitacoes).findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(1L, status);
        verify(solicitacoes, never()).findByLocadoraIdOrderByDataCriacaoAscIdAsc(any());
    }

    @Test
    void retornaFilaVaziaSemFiltro() {
        prepararLocadora();

        when(solicitacoes.findByLocadoraIdOrderByDataCriacaoAscIdAsc(1L))
                .thenReturn(List.of());

        assertThat(service.listar(null)).isEmpty();

        verify(solicitacoes).findByLocadoraIdOrderByDataCriacaoAscIdAsc(1L);
        verify(solicitacoes, never()).findByLocadoraIdAndStatusOrderByDataCriacaoAscIdAsc(any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void impedeConsultaComLocadoraInvalida(Long locadoraId) {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);

        when(parametros.id()).thenReturn(locadoraId);
        when(locadoraApi.obterParametros()).thenReturn(parametros);

        Class<? extends RuntimeException> excecao = locadoraId == null
                ? NullPointerException.class
                : IllegalStateException.class;

        assertThatThrownBy(() -> service.listar(null))
                .isInstanceOf(excecao);

        verifyNoInteractions(solicitacoes);
    }

    @Test
    void utilizaIdentificadorDaLocadoraFornecidoPelaApi() {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);

        when(parametros.id()).thenReturn(42L);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
        when(solicitacoes.findByLocadoraIdOrderByDataCriacaoAscIdAsc(42L))
                .thenReturn(List.of());

        assertThat(service.listar(null)).isEmpty();

        verify(solicitacoes).findByLocadoraIdOrderByDataCriacaoAscIdAsc(42L);
    }

    @Test
    void montaResumoSemAcessarItensDaSolicitacao() {
        prepararLocadora();

        Solicitacao solicitacao = mock(Solicitacao.class);

        when(solicitacao.getId()).thenReturn(7L);
        when(solicitacao.getStatus()).thenReturn(StatusSolicitacao.PENDENTE);
        when(solicitacoes.findByLocadoraIdOrderByDataCriacaoAscIdAsc(1L))
                .thenReturn(List.of(solicitacao));

        assertThat(service.listar(null))
                .extracting(SolicitacaoResumo::id)
                .containsExactly(7L);

        verify(solicitacao, never()).getItens();
    }

    // ---------- RD011: cliente inadimplente ou inativo ----------

    @Test
    void bloqueiaClienteInadimplente_RD011() {
        prepararLocadora();
        prepararCliente(true, false);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RD011");

        verify(clienteApi).consultarResumo(10L);
        verify(clienteApi, never()).verificarAdimplencia(any());
        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void bloqueiaClienteInativo() {
        prepararLocadora();
        prepararCliente(false, true);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void bloqueiaClienteInativoEInadimplente() {
        prepararLocadora();
        prepararCliente(false, false);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void clienteInexistenteResultaEmNaoEncontrado() {
        prepararLocadora();

        when(clienteApi.consultarResumo(10L))
                .thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado com id: 10"));

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void bloqueiaClienteDeOutraLocadora() {
        prepararLocadora();
        prepararCliente(2L, true, StatusAdimplencia.ADIMPLENTE);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Cliente não encontrado");

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void bloqueiaClienteSemLocadora() {
        prepararLocadora();
        prepararCliente(null, true, StatusAdimplencia.ADIMPLENTE);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @Test
    void bloqueiaClienteComStatusAdimplenciaIndefinido() {
        prepararLocadora();
        prepararCliente(1L, true, null);

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RD011");

        verifyNoInteractions(solicitacoes, unidadeApi);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void impedeRegistroComLocadoraInvalida(Long locadoraId) {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);

        when(parametros.id()).thenReturn(locadoraId);
        when(locadoraApi.obterParametros()).thenReturn(parametros);

        Class<? extends RuntimeException> excecao = locadoraId == null
                ? NullPointerException.class
                : IllegalStateException.class;

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(excecao);

        verifyNoInteractions(clienteApi, unidadeApi, solicitacoes);
    }

    @Test
    void clienteRegularizadoPodeRegistrarNovaSolicitacao() {
        prepararLocadora();

        ClienteResumoDTO inadimplente = new ClienteResumoDTO(
                10L, 1L, "Construtora", "12345678000199", "Ana",
                "11122233344", "ana@teste.com", "79999999999",
                StatusAdimplencia.INADIMPLENTE, true,
                "49100000", "Rua B", "20", null, "Centro", "São Cristóvão", "SE");

        ClienteResumoDTO adimplente = new ClienteResumoDTO(
                10L, 1L, "Construtora", "12345678000199", "Ana",
                "11122233344", "ana@teste.com", "79999999999",
                StatusAdimplencia.ADIMPLENTE, true,
                "49100000", "Rua B", "20", null, "Centro", "São Cristóvão", "SE");

        when(clienteApi.consultarResumo(10L))
                .thenReturn(inadimplente, adimplente);

        RegistrarSolicitacaoRequest pedido = request(
                HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1));

        assertThatThrownBy(() -> service.registrar(pedido, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RD011");

        verifyNoInteractions(solicitacoes, unidadeApi);

        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(true)));
        when(solicitacoes.saveAndFlush(any(Solicitacao.class)))
                .thenAnswer(i -> i.getArgument(0));

        SolicitacaoResponse resposta = service.registrar(pedido, 20L);

        assertThat(resposta.status()).isEqualTo(StatusSolicitacao.PENDENTE);
        assertThat(resposta.valorEstimado()).isEqualByComparingTo("600.00");

        verify(solicitacoes).saveAndFlush(any(Solicitacao.class));
        verify(clienteApi, never()).verificarAdimplencia(any());
    }
}