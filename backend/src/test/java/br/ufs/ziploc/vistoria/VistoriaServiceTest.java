package br.ufs.ziploc.vistoria;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VistoriaServiceTest {

    private static final Long LOCADORA_ID = 1L;
    private static final Long DEVOLUCAO_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Long UNIDADE_ID = 5L;
    private static final Long TECNICO_ID = 7L;
    private static final Instant INICIO = Instant.parse("2020-01-01T00:00:00Z");

    @Mock VistoriaRepository vistorias;
    @Mock DevolucaoApi devolucaoApi;
    @Mock ContratoApi contratoApi;
    @Mock UnidadeApi unidadeApi;
    @Mock ManutencaoApi manutencaoApi;
    @InjectMocks VistoriaService service;

    private void devolucaoEItemExistem() {
        when(devolucaoApi.buscarPorId(DEVOLUCAO_ID)).thenReturn(Optional.of(
                new DevolucaoResumo(DEVOLUCAO_ID, ITEM_ID, 3L, Instant.now(), BigDecimal.ZERO, null)));
        LocalDate hoje = LocalDate.now();
        when(contratoApi.buscarItemContrato(ITEM_ID)).thenReturn(Optional.of(
                new ItemContratoPrazo(ITEM_ID, 100L, 200L, UNIDADE_ID, hoje, hoje, hoje, hoje,
                        new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("500.00"))));
    }

    private void semVistoriaAnterior() {
        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID)).thenReturn(Optional.empty());
    }

    private void unidadeNoStatus(String status) {
        when(unidadeApi.buscarUnidade(UNIDADE_ID)).thenReturn(Optional.of(
                new UnidadeInfo(UNIDADE_ID, 1L, "SN-1", status, true, new BigDecimal("50.00"))));
    }

    private void salvarRetornandoArgumento() {
        when(vistorias.saveAndFlush(any(Vistoria.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrar_devolucaoInexistente_lancaRecursoNaoEncontrado() {
        when(devolucaoApi.buscarPorId(DEVOLUCAO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void registrar_reprovadaSemDescricaoDeAvarias_RD022_lancaIllegalArgument() {
        devolucaoEItemExistem();
        semVistoriaAnterior();

        assertThatThrownBy(() -> service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.REPROVADA, "  ", TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(vistorias, never()).saveAndFlush(any());
    }

    @Test
    void registrar_vistoriaJaConcluida_lancaIllegalState_eNaoSalva() {
        devolucaoEItemExistem();
        Vistoria concluida = new Vistoria(LOCADORA_ID, DEVOLUCAO_ID, TECNICO_ID, INICIO);
        concluida.concluir(ResultadoVistoria.APROVADA, null, INICIO.plusSeconds(60));
        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID)).thenReturn(Optional.of(concluida));

        assertThatThrownBy(() -> service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(vistorias, never()).saveAndFlush(any());
        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_unidadeForaDeVistoria_lancaIllegalState() {
        devolucaoEItemExistem();
        semVistoriaAnterior();
        unidadeNoStatus("DISPONIVEL");

        assertThatThrownBy(() -> service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(vistorias, never()).saveAndFlush(any());
    }

    @Test
    void registrar_aprovada_salvaVistoria_semManutencao_eNaoMudaStatusPelaApi() {
        devolucaoEItemExistem();
        semVistoriaAnterior();
        unidadeNoStatus("EM_VISTORIA");
        salvarRetornandoArgumento();

        var dto = service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.APROVADA, null, TECNICO_ID);

        verify(unidadeApi, never()).liberar(any()); // quem libera a unidade é o trigger do banco
        verify(unidadeApi, never()).iniciarManutencao(any());
        verifyNoInteractions(manutencaoApi);
        assertThat(dto.resultado()).isEqualTo(ResultadoVistoria.APROVADA);
        assertThat(dto.tecnicoId()).isEqualTo(TECNICO_ID);
        assertThat(dto.dataConclusao()).isNotNull();
    }

    @Test
    void registrar_reprovada_abreManutencao_eNaoMudaStatusPelaApi() {
        devolucaoEItemExistem();
        semVistoriaAnterior();
        unidadeNoStatus("EM_VISTORIA");
        salvarRetornandoArgumento();

        var dto = service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.REPROVADA, "Tela quebrada", TECNICO_ID);

        verify(unidadeApi, never()).iniciarManutencao(any()); // o trigger do banco põe a unidade em EM_MANUTENCAO
        verify(unidadeApi, never()).liberar(any());
        verify(manutencaoApi).abrirAPartirDeVistoria(LOCADORA_ID, UNIDADE_ID, TECNICO_ID, "Tela quebrada");
        assertThat(dto.descricaoAvarias()).isEqualTo("Tela quebrada");
    }

    @Test
    void registrar_vistoriaIniciadaEPendente_concluiAExistente() {
        devolucaoEItemExistem();
        Vistoria pendente = new Vistoria(LOCADORA_ID, DEVOLUCAO_ID, TECNICO_ID, INICIO);
        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID)).thenReturn(Optional.of(pendente));
        unidadeNoStatus("EM_VISTORIA");
        salvarRetornandoArgumento();

        var dto = service.registrar(LOCADORA_ID, DEVOLUCAO_ID, ResultadoVistoria.APROVADA, null, TECNICO_ID);

        assertThat(dto.dataInicio()).isEqualTo(INICIO);
        assertThat(dto.dataConclusao()).isNotNull();
    }
}