package br.ufs.ziploc.vistoria.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VistoriaTest {

    private static final Instant INICIO =
            Instant.parse("2026-01-01T12:00:00Z");

    private Vistoria vistoria() {
        return new Vistoria(1L, 2L, 3L, INICIO);
    }

    @Test
    void deveNascerPendente() {
        Vistoria v = vistoria();

        assertThat(v.getLocadoraId()).isEqualTo(1L);
        assertThat(v.getDevolucaoId()).isEqualTo(2L);
        assertThat(v.getTecnicoId()).isEqualTo(3L);
        assertThat(v.getDataInicio()).isEqualTo(INICIO);
        assertThat(v.getResultado()).isNull();
        assertThat(v.getDataConclusao()).isNull();
        assertThat(v.estaConcluida()).isFalse();
        assertThat(v.reprovada()).isFalse();
    }

    @Test
    void deveAprovarSemDescricao() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.APROVADA,
                null, INICIO.plusSeconds(60));

        assertThat(v.estaConcluida()).isTrue();
        assertThat(v.reprovada()).isFalse();
        assertThat(v.getResultado())
                .isEqualTo(ResultadoVistoria.APROVADA);
    }

    @Test
    void deveReprovarComDescricaoNormalizada() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.REPROVADA,
                "  Motor danificado  ",
                INICIO.plusSeconds(60));

        assertThat(v.estaConcluida()).isTrue();
        assertThat(v.reprovada()).isTrue();
        assertThat(v.getDescricaoAvarias())
                .isEqualTo("Motor danificado");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void deveExigirDescricaoNaReprovacao(String descricao) {
        Vistoria v = vistoria();

        assertThatThrownBy(() -> v.concluir(
                ResultadoVistoria.REPROVADA,
                descricao, INICIO.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RD022");

        assertThat(v.estaConcluida()).isFalse();
        assertThat(v.getResultado()).isNull();
    }

    @Test
    void deveRejeitarResultadoNulo() {
        assertThatThrownBy(() -> vistoria().concluir(
                null, null, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarConclusaoSemData() {
        assertThatThrownBy(() -> vistoria().concluir(
                ResultadoVistoria.APROVADA, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarConclusaoAnteriorAoInicio() {
        Vistoria v = vistoria();

        assertThatThrownBy(() -> v.concluir(
                ResultadoVistoria.APROVADA,
                null, INICIO.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(v.estaConcluida()).isFalse();
    }

    @Test
    void deveAceitarConclusaoNoMesmoInstanteDoInicio() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.APROVADA,
                null, INICIO);

        assertThat(v.getDataConclusao()).isEqualTo(INICIO);
    }

    @Test
    void devePermitirConclusaoApos48Horas() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.REPROVADA,
                "Avaria identificada",
                INICIO.plusSeconds(49 * 3600L));

        assertThat(v.reprovada()).isTrue();
        assertThat(v.estaConcluida()).isTrue();
    }

    @Test
    void naoDevePermitirAlterarVistoriaConcluida() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.APROVADA,
                null, INICIO.plusSeconds(60));

        assertThatThrownBy(() -> v.concluir(
                ResultadoVistoria.REPROVADA,
                "Avaria", INICIO.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(v.getResultado())
                .isEqualTo(ResultadoVistoria.APROVADA);
    }

    @Test
    void deveRejeitarDescricaoAcimaDoLimite() {
        assertThatThrownBy(() -> vistoria().concluir(
                ResultadoVistoria.REPROVADA,
                "A".repeat(2001), INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveAceitarDescricaoNoLimite() {
        Vistoria v = vistoria();

        v.concluir(ResultadoVistoria.REPROVADA,
                "A".repeat(2000), INICIO);

        assertThat(v.getDescricaoAvarias()).hasSize(2000);
    }

    @Test
    void deveRejeitarLocadoraInvalida() {
        assertThatThrownBy(() ->
                new Vistoria(null, 2L, 3L, INICIO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                new Vistoria(0L, 2L, 3L, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarDevolucaoInvalida() {
        assertThatThrownBy(() ->
                new Vistoria(1L, null, 3L, INICIO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                new Vistoria(1L, -1L, 3L, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarTecnicoInvalido() {
        assertThatThrownBy(() ->
                new Vistoria(1L, 2L, null, INICIO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                new Vistoria(1L, 2L, 0L, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarDataInicioNula() {
        assertThatThrownBy(() ->
                new Vistoria(1L, 2L, 3L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}