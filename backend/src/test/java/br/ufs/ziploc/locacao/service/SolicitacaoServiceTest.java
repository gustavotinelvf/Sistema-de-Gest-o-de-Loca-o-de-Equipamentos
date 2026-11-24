package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeApi;
import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.disponibilidade.api.ItemPedidoDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import br.ufs.ziploc.locacao.dto.ItemContratoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {
    @Mock
    private SolicitacaoRepository solicitacaoRepository;
    @Mock
    private ContratoRepository contratoRepository;
    @Mock
    private DisponibilidadeApi disponibilidadeApi;
    @InjectMocks
    private SolicitacaoService service;
    private Solicitacao solicitacao;

    @BeforeEach
    void preparar() {
        solicitacao = new Solicitacao(1L, 2L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10));
        solicitacao.adicionarItem(30L, 2);
        ReflectionTestUtils.setField(solicitacao, "id", 7L);
    }

    @Test
    void aprovaECriaContratoPlanejado() {
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        when(disponibilidadeApi.alocarUnidades(List.of(new ItemPedidoDTO(30L, 2)), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10))).thenReturn(List.of(new UnidadeDTO(100L, 30L, new BigDecimal("50.00")), new UnidadeDTO(101L, 30L, new BigDecimal("50.00"))));
        when(contratoRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        ContratoDTO dto = service.aprovar(7L, 5L, new BigDecimal("10.00"));
        assertThat(dto.status()).isEqualTo("PLANEJADO");
        assertThat(dto.solicitacaoId()).isEqualTo(7L);
        assertThat(dto.valorTotal()).isEqualByComparingTo("1000.00");
        assertThat(dto.percentualMulta()).isEqualByComparingTo("10.00");
        assertThat(dto.itens()).extracting(ItemContratoDTO::unidadeId).containsExactly(100L, 101L);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.APROVADA);
        assertThat(solicitacao.getAnalisadoPorId()).isEqualTo(5L);
    }

    @Test
    void naoCriaContratoSemUnidades() {
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        when(disponibilidadeApi.alocarUnidades(any(), any(), any())).thenThrow(new IndisponibilidadeException(List.of(new IndisponibilidadeException.Falta(30L, 2, 1))));
        assertThatThrownBy(() -> service.aprovar(7L, 5L, BigDecimal.TEN)).isInstanceOf(IndisponibilidadeException.class);
        verifyNoInteractions(contratoRepository);
    }

    @Test
    void naoAprovaSolicitacaoJaAnalisada() {
        solicitacao.rejeitar(5L, "Motivo");
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.aprovar(7L, 5L, BigDecimal.TEN)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(disponibilidadeApi, contratoRepository);
    }

    @Test
    void reprovaSolicitacaoPendente() {
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        SolicitacaoDTO dto = service.reprovar(7L, 5L, "  Cliente sem garantia  ");
        assertThat(dto.status()).isEqualTo("REJEITADA");
        assertThat(dto.justificativa()).isEqualTo("Cliente sem garantia");
        assertThat(dto.analisadoPorId()).isEqualTo(5L);
        assertThat(dto.dataAnalise()).isNotNull();
        assertThat(dto.itens()).singleElement().satisfies(i -> assertThat(i.quantidade()).isEqualTo(2));
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.REJEITADA);
    }

    @Test
    void exigeJustificativa() {
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.reprovar(7L, 5L, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);
    }

    @Test
    void recusaSolicitacaoJaAnalisada() {
        solicitacao.aprovar(5L);
        when(solicitacaoRepository.findComTravaById(7L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.reprovar(7L, 5L, "Motivo")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaQuandoSolicitacaoNaoExiste() {
        when(solicitacaoRepository.findComTravaById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reprovar(99L, 5L, "Motivo")).isInstanceOf(SolicitacaoNaoEncontradaException.class);
    }
}
