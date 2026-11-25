package br.ufs.ziploc.vistoria;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.domain.StatusVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import br.ufs.ziploc.vistoria.service.VistoriaApiImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VistoriaApiImplTest {

    @Mock VistoriaRepository vistorias;
    @Mock DevolucaoApi devolucaoApi;
    @Mock ContratoApi contratoApi;
    @InjectMocks VistoriaApiImpl api;

    private static DevolucaoResumo devolucao(Long id, Long itemId) {
        return new DevolucaoResumo(id, itemId, 3L, Instant.now(), BigDecimal.ZERO, null);
    }

    private static Vistoria vistoriaComDano(Long devolucaoId, String custo) {
        return new Vistoria(devolucaoId, 7L, Instant.now(), StatusVistoria.REPROVADA_COM_DANOS,
                new BigDecimal(custo), "laudo");
    }

    private static ItemContratoPrazo item(Long id) {
        LocalDate hoje = LocalDate.now();
        return new ItemContratoPrazo(id, 100L, 200L, 5L, hoje, hoje, hoje, hoje,
                new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("500.00"));
    }

    @Test
    void totalizarDanosDoItem_semDevolucao_retornaZero() {
        when(devolucaoApi.buscarPorItemContratoId(1L)).thenReturn(Optional.empty());

        assertThat(api.totalizarDanosDoItem(1L)).isEqualByComparingTo("0");
    }

    @Test
    void totalizarDanosDoItem_devolvidoMasSemVistoria_retornaZero() {
        when(devolucaoApi.buscarPorItemContratoId(1L)).thenReturn(Optional.of(devolucao(11L, 1L)));
        when(vistorias.findByDevolucaoId(11L)).thenReturn(Optional.empty());

        assertThat(api.totalizarDanosDoItem(1L)).isEqualByComparingTo("0");
    }

    @Test
    void totalizarDanosDoItem_comVistoria_retornaCustoDoDano() {
        when(devolucaoApi.buscarPorItemContratoId(1L)).thenReturn(Optional.of(devolucao(11L, 1L)));
        when(vistorias.findByDevolucaoId(11L)).thenReturn(Optional.of(vistoriaComDano(11L, "100.00")));

        assertThat(api.totalizarDanosDoItem(1L)).isEqualByComparingTo("100.00");
    }

    @Test
    void totalizarDanosDoContrato_somaDanosDeTodosOsItens() {
        ContratoResumo contrato = new ContratoResumo(100L, 200L, "ATIVO", LocalDate.now(), LocalDate.now(),
                new BigDecimal("500.00"), new BigDecimal("10.00"), List.of(item(1L), item(2L)));
        when(contratoApi.buscarContrato(100L)).thenReturn(Optional.of(contrato));
        when(devolucaoApi.buscarPorItemContratoId(1L)).thenReturn(Optional.of(devolucao(11L, 1L)));
        when(devolucaoApi.buscarPorItemContratoId(2L)).thenReturn(Optional.of(devolucao(12L, 2L)));
        when(vistorias.findByDevolucaoId(11L)).thenReturn(Optional.of(vistoriaComDano(11L, "100.00")));
        when(vistorias.findByDevolucaoId(12L)).thenReturn(Optional.of(vistoriaComDano(12L, "50.50")));

        assertThat(api.totalizarDanosDoContrato(100L)).isEqualByComparingTo("150.50");
    }

    @Test
    void totalizarDanosDoContrato_contratoInexistente_lancaRecursoNaoEncontrado() {
        when(contratoApi.buscarContrato(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.totalizarDanosDoContrato(100L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}