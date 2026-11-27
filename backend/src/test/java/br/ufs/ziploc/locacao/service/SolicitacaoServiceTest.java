package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.ModeloInfo;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.RegistrarSolicitacaoRequest;
import br.ufs.ziploc.locacao.dto.SolicitacaoResponse;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Mock SolicitacaoRepository solicitacoes;
    @Mock UnidadeApi unidadeApi;
    @Mock LocadoraApi locadoraApi;
    @InjectMocks SolicitacaoService service;

    private void prepararLocadora() {
        ParametrosLocadoraDTO parametros = mock(ParametrosLocadoraDTO.class);
        when(parametros.id()).thenReturn(1L);
        when(locadoraApi.obterParametros()).thenReturn(parametros);
    }

    private ModeloInfo modelo(boolean ativo) {
        return new ModeloInfo(1L, "Betoneira", "Construção", new BigDecimal("100.00"), 1, 5, ativo);
    }

    private RegistrarSolicitacaoRequest request(LocalDate inicio, RegistrarSolicitacaoRequest.Item... itens) {
        return new RegistrarSolicitacaoRequest(10L, inicio, inicio.plusDays(5), List.of(itens));
    }

    @Test
    void registraSolicitacaoPendenteComItens() {
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(true)));
        when(solicitacoes.saveAndFlush(any(Solicitacao.class))).thenAnswer(i -> i.getArgument(0));

        SolicitacaoResponse r = service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 3)), 20L);

        assertThat(r.status()).isEqualTo(StatusSolicitacao.PENDENTE);
        assertThat(r.itens()).hasSize(1);
        assertThat(r.itens().get(0).quantidade()).isEqualTo(3);
        assertThat(r.clienteId()).isEqualTo(10L);
        assertThat(r.valorEstimado()).isEqualByComparingTo("1800.00");
    }

    @Test
    void rejeitaModeloInativo() {
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(false)));

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaModeloInexistente() {
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void rejeitaModeloRepetidoNosItens() {
        prepararLocadora();
        when(unidadeApi.buscarModelo(1L)).thenReturn(Optional.of(modelo(true)));

        assertThatThrownBy(() -> service.registrar(request(HOJE.plusDays(1),
                new RegistrarSolicitacaoRequest.Item(1L, 1), new RegistrarSolicitacaoRequest.Item(1L, 2)), 20L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaDataDeInicioNoPassado() {
        assertThatThrownBy(() -> service.registrar(request(HOJE.minusDays(1), new RegistrarSolicitacaoRequest.Item(1L, 1)), 20L))
                .isInstanceOf(IllegalArgumentException.class);
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
}