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
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.domain.Vistoria;
import br.ufs.ziploc.vistoria.repository.VistoriaRepository;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VistoriaServiceTest {

    private static final Long LOCADORA_ID = 1L;
    private static final Long DEVOLUCAO_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Long UNIDADE_ID = 5L;
    private static final Long TECNICO_ID = 7L;

    private static final Instant DEVOLVIDA_EM =
            Instant.parse("2020-01-01T00:00:00Z");

    private static final Instant INICIO =
            DEVOLVIDA_EM.plusSeconds(60);

    @Mock VistoriaRepository vistorias;
    @Mock DevolucaoApi devolucaoApi;
    @Mock ContratoApi contratoApi;
    @Mock UnidadeApi unidadeApi;
    @Mock ManutencaoApi manutencaoApi;

    @InjectMocks VistoriaService service;

    private void devolucaoExiste() {
        when(devolucaoApi.buscarPorIdParaAtualizacao(
                DEVOLUCAO_ID, LOCADORA_ID))
                .thenReturn(Optional.of(
                        new DevolucaoResumo(
                                DEVOLUCAO_ID,
                                ITEM_ID,
                                3L,
                                DEVOLVIDA_EM,
                                BigDecimal.ZERO,
                                null)));
    }

    private void itemExiste() {
        LocalDate hoje = LocalDate.now();

        when(contratoApi.buscarItemContrato(ITEM_ID))
                .thenReturn(Optional.of(
                        new ItemContratoPrazo(
                                ITEM_ID, 100L, 200L, UNIDADE_ID,
                                hoje, hoje, hoje, hoje,
                                new BigDecimal("50.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("500.00"))));
    }

    private void semVistoriaAnterior() {
        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID))
                .thenReturn(Optional.empty());
    }

    private void unidadeNoStatus(String status) {
        when(unidadeApi.buscarUnidadeParaAtualizacao(UNIDADE_ID))
                .thenReturn(Optional.of(
                        new UnidadeInfo(
                                UNIDADE_ID, 1L, "SN-1",
                                status, true,
                                new BigDecimal("50.00"))));
    }

    private void prepararNovaVistoria() {
        devolucaoExiste();
        semVistoriaAnterior();
        itemExiste();
        unidadeNoStatus("EM_VISTORIA");
    }

    private void salvarRetornandoArgumento() {
        when(vistorias.saveAndFlush(any(Vistoria.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private void verificarSemGravacao() {
        verify(vistorias, never()).saveAndFlush(any());
        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_devolucaoInexistente_lancaNaoEncontrado() {
        when(devolucaoApi.buscarPorIdParaAtualizacao(
                DEVOLUCAO_ID, LOCADORA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(vistorias, contratoApi, unidadeApi, manutencaoApi);
    }

    @Test
    void registrar_devolucaoDeOutraLocadora_lancaNaoEncontrado() {
        when(devolucaoApi.buscarPorIdParaAtualizacao(
                DEVOLUCAO_ID, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                99L, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(vistorias, contratoApi, unidadeApi, manutencaoApi);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t\n"})
    void registrar_reprovadaSemDescricao_RD022(String descricao) {
        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.REPROVADA,
                descricao, TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RD022");

        verifyNoInteractions(
                devolucaoApi, vistorias, contratoApi,
                unidadeApi, manutencaoApi);
    }

    @Test
    void registrar_resultadoNulo_lancaIllegalArgument() {
        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                null, null, TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(
                devolucaoApi, vistorias, contratoApi,
                unidadeApi, manutencaoApi);
    }

    @Test
    void registrar_descricaoAcimaDoLimite_lancaIllegalArgument() {
        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.REPROVADA,
                "A".repeat(2001), TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(
                devolucaoApi, vistorias, contratoApi,
                unidadeApi, manutencaoApi);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void registrar_locadoraInvalida_lancaIllegalArgument(long id) {
        assertThatThrownBy(() -> service.registrar(
                id, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(devolucaoApi, vistorias);
    }

    @Test
    void registrar_vistoriaJaConcluida_lancaIllegalState() {
        devolucaoExiste();

        Vistoria concluida = new Vistoria(
                LOCADORA_ID, DEVOLUCAO_ID, TECNICO_ID, INICIO);

        concluida.concluir(
                ResultadoVistoria.APROVADA,
                null, INICIO.plusSeconds(60));

        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID))
                .thenReturn(Optional.of(concluida));

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi concluída");

        verificarSemGravacao();
        verifyNoInteractions(contratoApi, unidadeApi);
    }

    @Test
    void registrar_vistoriaDeOutroTecnico_lancaIllegalState() {
        devolucaoExiste();

        Vistoria pendente = new Vistoria(
                LOCADORA_ID, DEVOLUCAO_ID, 99L, INICIO);

        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID))
                .thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(pendente.estaConcluida()).isFalse();
        verificarSemGravacao();
    }

    @Test
    void registrar_itemInexistente_lancaNaoEncontrado() {
        devolucaoExiste();
        semVistoriaAnterior();

        when(contratoApi.buscarItemContrato(ITEM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verificarSemGravacao();
        verifyNoInteractions(unidadeApi);
    }

    @Test
    void registrar_unidadeInexistente_lancaNaoEncontrado() {
        devolucaoExiste();
        semVistoriaAnterior();
        itemExiste();

        when(unidadeApi.buscarUnidadeParaAtualizacao(UNIDADE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verificarSemGravacao();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DISPONIVEL", "LOCADA", "EM_MANUTENCAO"
    })
    void registrar_unidadeForaDeVistoria_lancaIllegalState(String status) {
        devolucaoExiste();
        semVistoriaAnterior();
        itemExiste();
        unidadeNoStatus(status);

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(status);

        verificarSemGravacao();
    }

    @Test
    void registrar_unidadeForaDeVistoria_naoModificaVistoriaPendente() {
        devolucaoExiste();
        itemExiste();
        unidadeNoStatus("DISPONIVEL");

        Vistoria pendente = new Vistoria(
                LOCADORA_ID, DEVOLUCAO_ID, TECNICO_ID, INICIO);

        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID))
                .thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(pendente.estaConcluida()).isFalse();
        assertThat(pendente.getResultado()).isNull();

        verificarSemGravacao();
    }

    @Test
    void registrar_aprovada_salvaSemManutencao() {
        prepararNovaVistoria();
        salvarRetornandoArgumento();

        var dto = service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID);

        assertThat(dto.resultado())
                .isEqualTo(ResultadoVistoria.APROVADA);

        assertThat(dto.devolucaoId()).isEqualTo(DEVOLUCAO_ID);
        assertThat(dto.tecnicoId()).isEqualTo(TECNICO_ID);
        assertThat(dto.dataConclusao()).isNotNull();
        assertThat(dto.descricaoAvarias()).isNull();

        verify(vistorias).saveAndFlush(any(Vistoria.class));
        verify(unidadeApi, never()).liberar(any());
        verify(unidadeApi, never()).iniciarManutencao(any());
        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_reprovada_abreManutencaoAposGravarVistoria() {
        prepararNovaVistoria();
        salvarRetornandoArgumento();

        var dto = service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.REPROVADA,
                "  Tela quebrada  ", TECNICO_ID);

        assertThat(dto.resultado())
                .isEqualTo(ResultadoVistoria.REPROVADA);

        assertThat(dto.descricaoAvarias())
                .isEqualTo("Tela quebrada");

        var ordem = inOrder(vistorias, manutencaoApi);

        ordem.verify(vistorias)
                .saveAndFlush(any(Vistoria.class));

        ordem.verify(manutencaoApi)
                .abrirAPartirDeVistoria(
                        LOCADORA_ID, UNIDADE_ID,
                        TECNICO_ID, "Tela quebrada");

        verify(unidadeApi, never()).iniciarManutencao(any());
        verify(unidadeApi, never()).liberar(any());
    }

    @Test
    void registrar_descricaoCom2000Caracteres_aceita() {
        prepararNovaVistoria();
        salvarRetornandoArgumento();

        String descricao = "A".repeat(2000);

        var dto = service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.REPROVADA,
                descricao, TECNICO_ID);

        assertThat(dto.descricaoAvarias()).hasSize(2000);

        verify(manutencaoApi)
                .abrirAPartirDeVistoria(
                        LOCADORA_ID, UNIDADE_ID,
                        TECNICO_ID, descricao);
    }

    @Test
    void registrar_vistoriaPendente_concluiMesmoApos48Horas() {
        devolucaoExiste();
        itemExiste();
        unidadeNoStatus("EM_VISTORIA");
        salvarRetornandoArgumento();

        Vistoria pendente = new Vistoria(
                LOCADORA_ID, DEVOLUCAO_ID,
                TECNICO_ID, INICIO);

        when(vistorias.findByDevolucaoId(DEVOLUCAO_ID))
                .thenReturn(Optional.of(pendente));

        var dto = service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID);

        assertThat(dto.dataInicio()).isEqualTo(INICIO);
        assertThat(dto.dataConclusao()).isNotNull();
        assertThat(pendente.estaConcluida()).isTrue();

        verify(vistorias).saveAndFlush(pendente);
        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_devolucaoFutura_lancaIllegalState() {
        when(devolucaoApi.buscarPorIdParaAtualizacao(
                DEVOLUCAO_ID, LOCADORA_ID))
                .thenReturn(Optional.of(
                        new DevolucaoResumo(
                                DEVOLUCAO_ID, ITEM_ID, 3L,
                                Instant.now().plusSeconds(86400),
                                BigDecimal.ZERO, null)));

        semVistoriaAnterior();
        itemExiste();
        unidadeNoStatus("EM_VISTORIA");

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(IllegalStateException.class);

        verificarSemGravacao();
    }

    @Test
    void registrar_violacaoUnicaDaDevolucao_traduzParaConflito() {
        prepararNovaVistoria();

        var causa = new ConstraintViolationException(
                "Duplicidade",
                new SQLException("Duplicidade", "23505"),
                "vistoria_devolucao_id_key");

        when(vistorias.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "Erro de integridade", causa));

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isInstanceOf(ConflitoException.class);

        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_violacaoDeOutraConstraint_preservaExcecao() {
        prepararNovaVistoria();

        var causa = new ConstraintViolationException(
                "Duplicidade",
                new SQLException("Duplicidade", "23505"),
                "outra_constraint");

        var erro = new DataIntegrityViolationException(
                "Erro de integridade", causa);

        when(vistorias.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.APROVADA, null, TECNICO_ID))
                .isSameAs(erro);

        verifyNoInteractions(manutencaoApi);
    }

    @Test
    void registrar_falhaNaManutencao_propagaExcecao() {
        prepararNovaVistoria();
        salvarRetornandoArgumento();

        var erro = new IllegalStateException("Falha na manutenção");

        when(manutencaoApi.abrirAPartirDeVistoria(
                LOCADORA_ID, UNIDADE_ID,
                TECNICO_ID, "Motor danificado"))
                .thenThrow(erro);

        assertThatThrownBy(() -> service.registrar(
                LOCADORA_ID, DEVOLUCAO_ID,
                ResultadoVistoria.REPROVADA,
                "Motor danificado", TECNICO_ID))
                .isSameAs(erro);
    }
}