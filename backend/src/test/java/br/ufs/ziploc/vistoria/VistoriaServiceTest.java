package br.ufs.ziploc.vistoria;

import br.ufs.ziploc.devolucao.api.DevolucaoApi;
import br.ufs.ziploc.devolucao.api.DevolucaoResumo;
import br.ufs.ziploc.equipamento.api.UnidadeApi;
import br.ufs.ziploc.equipamento.api.UnidadeInfo;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.manutencao.api.ManutencaoApi;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.domain.StatusVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    private static final Long DEVOLUCAO_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Long UNIDADE_ID = 5L;
    private static final Long TECNICO_ID = 7L;

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

    private void unidadeNoStatus(String status) {
        when(unidadeApi.buscarUnidade(UNIDADE_ID)).thenReturn(Optional.of(
                new UnidadeInfo(UNIDADE_ID, 1L, "SN-1", status, true, new BigDecimal("50.00"))));
    }

    private void salvarComId() {
        when(vistorias.saveAndFlush(any(Vistoria.class))).thenAnswer(i -> {
            Vistoria v = i.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 99L);
            return v;
        });
    }

    @Test
    void registrar_devolucaoInexistente_lancaRecursoNaoEncontrado() {
        when(devolucaoApi.buscarPorId(DEVOLUCAO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(DEVOLUCAO_ID, StatusVistoria.APROVADA, null, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void registrar_reprovadaSemLaudo_RD022_lancaIllegalArgument() {
        devolucaoEItemExistem();

        assertThatThrownBy(() -> service.registrar(DEVOLUCAO_ID, StatusVistoria.REPROVADA_COM_DANOS,
                new BigDecimal("50"), "  ", TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(vistorias, never()).saveAndFlush(any());
    }

    @Test
    void registrar_aprovadaComCustoDeDano_lancaIllegalArgument() {
        devolucaoEItemExistem();

        assertThatThrownBy(() -> service.registrar(DEVOLUCAO_ID, StatusVistoria.APROVADA,
                new BigDecimal("10"), null, TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrar_vistoriaJaExistente_lancaConflito() {
        devolucaoEItemExistem();
        when(vistorias.existsByDevolucaoId(DEVOLUCAO_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(DEVOLUCAO_ID, StatusVistoria.APROVADA, null, null, TECNICO_ID))
                .isInstanceOf(ConflitoException.class);
        verify(unidadeApi, never()).liberar(any());
    }

    @Test
    void registrar_unidadeForaDeVistoria_lancaIllegalState() {
        devolucaoEItemExistem();
        unidadeNoStatus("DISPONIVEL");

        assertThatThrownBy(() -> service.registrar(DEVOLUCAO_ID, StatusVistoria.APROVADA, null, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(vistorias, never()).saveAndFlush(any());
    }

    @Test
    void registrar_aprovada_liberaUnidade_semManutencao() {
        devolucaoEItemExistem();
        unidadeNoStatus("EM_VISTORIA");
        salvarComId();

        var dto = service.registrar(DEVOLUCAO_ID, StatusVistoria.APROVADA, null, null, TECNICO_ID);

        verify(unidadeApi).liberar(UNIDADE_ID);
        verifyNoInteractions(manutencaoApi);
        assertThat(dto.custoDano()).isEqualByComparingTo("0");
        assertThat(dto.usuarioTecnicoId()).isEqualTo(TECNICO_ID);
    }

    @Test
    void registrar_reprovada_enviaUnidadeParaManutencao_eAbreManutencao() {
        devolucaoEItemExistem();
        unidadeNoStatus("EM_VISTORIA");
        salvarComId();

        var dto = service.registrar(DEVOLUCAO_ID, StatusVistoria.REPROVADA_COM_DANOS,
                new BigDecimal("150.5"), "Tela quebrada", TECNICO_ID);

        verify(unidadeApi).iniciarManutencao(UNIDADE_ID);
        verify(unidadeApi, never()).liberar(any());
        verify(manutencaoApi).abrirAPartirDeVistoria(99L, UNIDADE_ID, TECNICO_ID, "Tela quebrada");
        assertThat(dto.custoDano()).isEqualByComparingTo("150.50");
    }
}