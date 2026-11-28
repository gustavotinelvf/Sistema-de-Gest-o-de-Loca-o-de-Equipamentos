package br.ufs.ziploc.devolucao.service;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.exceptions.ItemContratoNaoEncontradoException;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import br.ufs.ziploc.faturamento.api.FaturaApi;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.shared.exception.ConflitoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucaoServiceTest {

    private static final Long LOCADORA_ID = 1L;
    private static final Long USUARIO_ID = 2L;
    private static final Long CONTRATO_ID = 3L;
    private static final Long ITEM_ID = 4L;
    private static final Long OUTRO_ITEM_ID = 5L;

    private static final ZoneId FUSO_LOCAL =
            ZoneId.of("America/Sao_Paulo");

    @Mock
    private DevolucaoRepository devolucaoRepository;

    @Mock
    private ContratoApi contratoApi;

    @Mock
    private FaturaApi faturaApi;

    @InjectMocks
    private DevolucaoService service;

    private ItemContratoPrazo itemParaCalculo(
            LocalDate prazo,
            String diaria,
            String percentual) {

        ItemContratoPrazo item =
                mock(ItemContratoPrazo.class);

        when(item.dataFimPrevista()).thenReturn(prazo);

        when(item.valorDiariaContratada())
                .thenReturn(new BigDecimal(diaria));

        when(item.percentualMulta())
                .thenReturn(new BigDecimal(percentual));

        return item;
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 0.00",
            "0, 0.00",
            "1, 0.00",
            "6, 0.00",
            "7, 0.00",
            "8, 10.00",
            "9, 20.00",
            "10, 30.00",
            "17, 100.00"
    })
    void deveCalcularMultaRespeitandoCarencia(
            long diasAtraso,
            String multaEsperada) {

        LocalDate prazo = LocalDate.of(2026, 11, 1);

        ItemContratoPrazo item =
                itemParaCalculo(prazo, "100.00", "10.00");

        BigDecimal multa =
                DevolucaoService.calcularMultaAtraso(
                        item,
                        prazo.plusDays(diasAtraso)
                );

        assertEquals(
                new BigDecimal(multaEsperada),
                multa
        );
    }

    @Test
    void deveUtilizarDiariaContratada() {

        LocalDate prazo = LocalDate.of(2026, 11, 1);

        ItemContratoPrazo item =
                itemParaCalculo(prazo, "250.00", "5.00");

        BigDecimal multa =
                DevolucaoService.calcularMultaAtraso(
                        item,
                        prazo.plusDays(10)
                );

        assertEquals(
                new BigDecimal("37.50"),
                multa
        );
    }

    @Test
    void deveArredondarMultaParaDuasCasasDecimais() {

        LocalDate prazo = LocalDate.of(2026, 11, 1);

        ItemContratoPrazo item =
                itemParaCalculo(prazo, "99.99", "2.50");

        BigDecimal multa =
                DevolucaoService.calcularMultaAtraso(
                        item,
                        prazo.plusDays(8)
                );

        assertEquals(
                new BigDecimal("2.50"),
                multa
        );
    }

    @Test
    void deveRetornarZeroQuandoPercentualForZero() {

        LocalDate prazo = LocalDate.of(2026, 11, 1);

        ItemContratoPrazo item =
                itemParaCalculo(prazo, "100.00", "0.00");

        BigDecimal multa =
                DevolucaoService.calcularMultaAtraso(
                        item,
                        prazo.plusDays(30)
                );

        assertEquals(
                new BigDecimal("0.00"),
                multa
        );
    }

    @Test
    void deveRejeitarItemInexistenteOuDeOutraLocadora() {

        when(contratoApi.buscarContratoParaDevolucao(
                ITEM_ID, LOCADORA_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ItemContratoNaoEncontradoException.class,
                () -> service.registrar(
                        new RegistrarDevolucaoDto(
                                ITEM_ID, null),
                        LOCADORA_ID,
                        USUARIO_ID
                )
        );

        verifyNoInteractions(
                devolucaoRepository,
                faturaApi
        );
    }

    @Test
    void deveImpedirDevolucaoDuplicada() {

        ContratoResumo contrato =
                mock(ContratoResumo.class);

        ItemContratoPrazo item =
                mock(ItemContratoPrazo.class);

        when(item.id()).thenReturn(ITEM_ID);

        when(contrato.itens())
                .thenReturn(List.of(item));

        when(contratoApi.buscarContratoParaDevolucao(
                ITEM_ID, LOCADORA_ID
        )).thenReturn(Optional.of(contrato));

        when(devolucaoRepository.findByItemContratoId(
                ITEM_ID
        )).thenReturn(
                Optional.of(mock(Devolucao.class))
        );

        assertThrows(
                ConflitoException.class,
                () -> service.registrar(
                        new RegistrarDevolucaoDto(
                                ITEM_ID, null),
                        LOCADORA_ID,
                        USUARIO_ID
                )
        );

        verify(devolucaoRepository, never())
                .saveAndFlush(any(Devolucao.class));

        verifyNoInteractions(faturaApi);
    }

    @Test
    void deveImpedirDevolucaoDeContratoNaoAtivo() {

        ContratoResumo contrato =
                mock(ContratoResumo.class);

        ItemContratoPrazo item =
                mock(ItemContratoPrazo.class);

        when(item.id()).thenReturn(ITEM_ID);

        when(contrato.itens())
                .thenReturn(List.of(item));

        when(contrato.status())
                .thenReturn("PLANEJADO");

        when(contratoApi.buscarContratoParaDevolucao(
                ITEM_ID, LOCADORA_ID
        )).thenReturn(Optional.of(contrato));

        when(devolucaoRepository.findByItemContratoId(
                ITEM_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.registrar(
                        new RegistrarDevolucaoDto(
                                ITEM_ID, null),
                        LOCADORA_ID,
                        USUARIO_ID
                )
        );

        verify(devolucaoRepository, never())
                .saveAndFlush(any(Devolucao.class));

        verifyNoInteractions(faturaApi);
    }

    @Test
    void deveRejeitarIdentificadorInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        new RegistrarDevolucaoDto(
                                -1L, null),
                        LOCADORA_ID,
                        USUARIO_ID
                )
        );

        verifyNoInteractions(
                contratoApi,
                devolucaoRepository,
                faturaApi
        );
    }

    @Test
    void deveRejeitarObservacoesAcimaDoLimite() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(
                        new RegistrarDevolucaoDto(
                                ITEM_ID,
                                "a".repeat(501)),
                        LOCADORA_ID,
                        USUARIO_ID
                )
        );

        verifyNoInteractions(
                contratoApi,
                devolucaoRepository,
                faturaApi
        );
    }

    private void prepararDevolucao(
            String valorContrato,
            String multaAnterior,
            long diasAtraso,
            long itensPendentes) {

        LocalDate hoje = LocalDate.now(FUSO_LOCAL);

        ItemContratoPrazo item =
                itemParaCalculo(
                        hoje.minusDays(diasAtraso),
                        "100.00",
                        "10.00"
                );

        when(item.id()).thenReturn(ITEM_ID);

        when(item.dataInicioReal())
                .thenReturn(hoje.minusDays(60));

        ItemContratoPrazo outroItem =
                mock(ItemContratoPrazo.class);

        when(outroItem.id())
                .thenReturn(OUTRO_ITEM_ID);

        ContratoResumo contrato =
                mock(ContratoResumo.class);

        when(contrato.id())
                .thenReturn(CONTRATO_ID);

        when(contrato.status())
                .thenReturn("ATIVO");

        when(contrato.valorTotal())
                .thenReturn(new BigDecimal(valorContrato));

        when(contrato.itens())
                .thenReturn(List.of(item, outroItem));

        when(contratoApi.buscarContratoParaDevolucao(
                ITEM_ID, LOCADORA_ID
        )).thenReturn(Optional.of(contrato));

        when(devolucaoRepository.findByItemContratoId(
                ITEM_ID
        )).thenReturn(Optional.empty());

        Devolucao anterior = Devolucao.builder()
                .multaAtraso(new BigDecimal(multaAnterior))
                .build();

        when(devolucaoRepository.findByItemContratoIdIn(
                List.of(ITEM_ID, OUTRO_ITEM_ID)
        )).thenReturn(List.of(anterior));

        when(devolucaoRepository.saveAndFlush(
                any(Devolucao.class)
        )).thenAnswer(invocacao ->
                invocacao.getArgument(0));

        when(contratoApi.contarItensPendentes(CONTRATO_ID))
                .thenReturn(itensPendentes);
    }

    @Test
    void deveLimitarMultaAoSaldoRestanteDoContrato() {

        prepararDevolucao(
                "500.00",
                "490.00",
                17,
                0
        );

        DevolucaoDto resultado = service.registrar(
                new RegistrarDevolucaoDto(
                        ITEM_ID,
                        "  Equipamento recebido.  "),
                LOCADORA_ID,
                USUARIO_ID
        );

        assertEquals(
                new BigDecimal("10.00"),
                resultado.multaAtraso()
        );

        assertEquals(
                "Equipamento recebido.",
                resultado.observacoes()
        );

        verify(faturaApi).gerarFatura(
                CONTRATO_ID,
                LOCADORA_ID,
                USUARIO_ID,
                new BigDecimal("500.00")
        );
    }

    @Test
    void deveZerarMultaQuandoTetoJaFoiAtingido() {

        prepararDevolucao(
                "500.00",
                "500.00",
                17,
                0
        );

        DevolucaoDto resultado = service.registrar(
                new RegistrarDevolucaoDto(
                        ITEM_ID, null),
                LOCADORA_ID,
                USUARIO_ID
        );

        assertEquals(
                new BigDecimal("0.00"),
                resultado.multaAtraso()
        );

        verify(faturaApi).gerarFatura(
                CONTRATO_ID,
                LOCADORA_ID,
                USUARIO_ID,
                new BigDecimal("500.00")
        );
    }

    @Test
    void naoDeveGerarFaturaEnquantoExistiremItensPendentes() {

        prepararDevolucao(
                "500.00",
                "0.00",
                8,
                1
        );

        DevolucaoDto resultado = service.registrar(
                new RegistrarDevolucaoDto(
                        ITEM_ID, "   "),
                LOCADORA_ID,
                USUARIO_ID
        );

        assertEquals(
                new BigDecimal("10.00"),
                resultado.multaAtraso()
        );

        assertNull(resultado.observacoes());

        verifyNoInteractions(faturaApi);
    }

    @Test
    void deveGerarFaturaConsolidadaNaUltimaDevolucao() {

        prepararDevolucao(
                "500.00",
                "30.00",
                9,
                0
        );

        DevolucaoDto resultado = service.registrar(
                new RegistrarDevolucaoDto(
                        ITEM_ID, null),
                LOCADORA_ID,
                USUARIO_ID
        );

        assertEquals(
                new BigDecimal("20.00"),
                resultado.multaAtraso()
        );

        verify(faturaApi, times(1)).gerarFatura(
                CONTRATO_ID,
                LOCADORA_ID,
                USUARIO_ID,
                new BigDecimal("50.00")
        );
    }
}