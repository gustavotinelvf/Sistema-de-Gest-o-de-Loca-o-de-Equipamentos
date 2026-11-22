package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.repository.SolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTest {
    @Mock
    private SolicitacaoRepository solicitacaoRepository;
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
