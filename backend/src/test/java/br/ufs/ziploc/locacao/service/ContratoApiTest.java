package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.locacao.domain.Contrato;
import br.ufs.ziploc.locacao.domain.ItemContrato;
import br.ufs.ziploc.locacao.domain.Solicitacao;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import br.ufs.ziploc.locacao.repository.ContratoRepository;
import br.ufs.ziploc.locacao.repository.ItemContratoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContratoApiTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 10);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    @Mock
    private ContratoRepository contratoRepository;
    @Mock
    private ItemContratoRepository itemContratoRepository;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private ContratoService service;
    private Contrato contrato;
    private ItemContrato item;

    @BeforeEach
    void preparar() {
        Solicitacao solicitacao = new Solicitacao(1L, 1L, 2L, INICIO, FIM);
        solicitacao.adicionarItem(30L, 1, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(solicitacao, "id", 6L);
        ReflectionTestUtils.setField(solicitacao.getItens().get(0), "id", 8L);
        solicitacao.aprovar(3L);
        contrato = new Contrato(solicitacao, new BigDecimal("1000.00"), new BigDecimal("2.00"), "Devolver os equipamentos no prazo contratado.");
        ReflectionTestUtils.setField(contrato, "id", 7L);
        contrato.adicionarItem(10L, 30L, new BigDecimal("100.00"));
        item = contrato.getItens().get(0);
        ReflectionTestUtils.setField(item, "id", 50L);
    }

    @Test
    void buscaContratoComItens() {
        when(contratoRepository.findById(7L)).thenReturn(Optional.of(contrato));
        ContratoResumo resumo = service.buscarContrato(7L).orElseThrow();
        assertThat(resumo.status()).isEqualTo("PLANEJADO");
        assertThat(resumo.clienteId()).isEqualTo(1L);
        assertThat(resumo.itens()).singleElement().satisfies(i -> assertThat(i.valorDiariaContratada()).isEqualByComparingTo("100.00"));
    }

    @Test
    void buscaItemComDadosDoContrato() {
        when(itemContratoRepository.findById(50L)).thenReturn(Optional.of(item));
        ItemContratoPrazo resumo = service.buscarItemContrato(50L).orElseThrow();
        assertThat(resumo.contratoId()).isEqualTo(7L);
        assertThat(resumo.unidadeId()).isEqualTo(10L);
        assertThat(resumo.dataFimPrevista()).isEqualTo(FIM);
        assertThat(resumo.percentualMulta()).isEqualByComparingTo("2.00");
        assertThat(resumo.valorLocacao()).isEqualByComparingTo("1000.00");
    }

    @Test
    void buscaItemComDevolucaoRegistrada() {
        contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        ReflectionTestUtils.setField(item, "dataDevolucaoReal", FIM);
        when(itemContratoRepository.findById(50L)).thenReturn(Optional.of(item));

        ItemContratoPrazo resumo = service.buscarItemContrato(50L).orElseThrow();

        assertThat(resumo.dataInicioReal()).isEqualTo(INICIO);
        assertThat(resumo.dataDevolucaoReal()).isEqualTo(FIM);
    }

    @Test
    void retornaVazioQuandoItemNaoExiste() {
        when(itemContratoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.buscarItemContrato(99L)).isEmpty();
    }

    @Test
    void contaItensPendentes() {
        when(contratoRepository.existsById(7L)).thenReturn(true);
        when(itemContratoRepository.countByContratoIdAndDataDevolucaoRealIsNull(7L)).thenReturn(1L);
        assertThat(service.contarItensPendentes(7L)).isEqualTo(1L);
    }

    @Test
    void finalizaContrato() {
        contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        ReflectionTestUtils.setField(item, "dataDevolucaoReal", FIM);
        when(contratoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(contrato));
        service.finalizarContrato(7L);
        verify(contratoRepository).findByIdForUpdate(7L);
        verify(entityManager).refresh(contrato);
        verify(contratoRepository).flush();
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.FINALIZADO);
    }

    @Test
    void falhaAoFinalizarContratoInexistente() {
        when(contratoRepository.findByIdForUpdate(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.finalizarContrato(8L)).isInstanceOf(ContratoNaoEncontradoException.class);
    }

    @Test
    void recusaFinalizacaoComItensPendentes() {
        contrato.ativar(INICIO.atStartOfDay(FUSO).toInstant(), 3L);
        when(contratoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.finalizarContrato(7L)).isInstanceOf(IllegalStateException.class);
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
    }
}