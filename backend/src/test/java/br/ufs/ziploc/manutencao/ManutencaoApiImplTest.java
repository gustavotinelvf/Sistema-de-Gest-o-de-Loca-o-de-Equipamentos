package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.manutencao.domain.Manutencao;
import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.repository.ManutencaoRepository;
import br.ufs.ziploc.manutencao.service.ManutencaoApiImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManutencaoApiImplTest {

    @Mock ManutencaoRepository manutencoes;
    @InjectMocks ManutencaoApiImpl api;

    @Test
    void abrirAPartirDeVistoria_semManutencaoAberta_criaCorretiva() {
        when(manutencoes.findFirstByUnidadeIdAndDataConclusaoIsNull(5L)).thenReturn(Optional.empty());
        when(manutencoes.save(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        var resumo = api.abrirAPartirDeVistoria(1L, 5L, 7L, "Tela quebrada");

        assertThat(resumo.unidadeId()).isEqualTo(5L);
        assertThat(resumo.tipo()).isEqualTo("CORRETIVA");
    }

    @Test
    void abrirAPartirDeVistoria_jaTemManutencaoAberta_naoCriaOutra() {
        Manutencao existente = new Manutencao(1L, 5L, 7L, TipoManutencao.CORRETIVA, "Tela quebrada", null,
                Instant.parse("2020-01-01T00:00:00Z"));
        when(manutencoes.findFirstByUnidadeIdAndDataConclusaoIsNull(5L)).thenReturn(Optional.of(existente));

        var resumo = api.abrirAPartirDeVistoria(1L, 5L, 7L, "Tela quebrada");

        verify(manutencoes, never()).save(any());
        assertThat(resumo.unidadeId()).isEqualTo(5L);
    }

    @Test
    void abrirAPartirDeVistoria_semDescricao_lancaIllegalArgument() {
        when(manutencoes.findFirstByUnidadeIdAndDataConclusaoIsNull(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.abrirAPartirDeVistoria(1L, 5L, 7L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(manutencoes, never()).save(any());
    }
}