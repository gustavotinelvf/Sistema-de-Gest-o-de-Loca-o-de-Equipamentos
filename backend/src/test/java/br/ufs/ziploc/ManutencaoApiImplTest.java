package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.manutencao.domain.Manutencao;
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
    void abrirAPartirDeVistoria_vistoriaNova_criaManutencaoEmAndamento() {
        when(manutencoes.findByVistoriaId(99L)).thenReturn(Optional.empty());
        when(manutencoes.save(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        var resumo = api.abrirAPartirDeVistoria(99L, 5L, 7L, "Tela quebrada");

        assertThat(resumo.vistoriaId()).isEqualTo(99L);
        assertThat(resumo.unidadeId()).isEqualTo(5L);
        assertThat(resumo.status()).isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void abrirAPartirDeVistoria_vistoriaJaTemManutencao_naoCriaOutra() {
        Manutencao existente = new Manutencao(5L, 99L, 7L, "Tela quebrada", Instant.parse("2020-01-01T00:00:00Z"));
        when(manutencoes.findByVistoriaId(99L)).thenReturn(Optional.of(existente));

        var resumo = api.abrirAPartirDeVistoria(99L, 5L, 7L, "Tela quebrada");

        verify(manutencoes, never()).save(any());
        assertThat(resumo.vistoriaId()).isEqualTo(99L);
    }

    @Test
    void abrirAPartirDeVistoria_semDescricao_lancaIllegalArgument() {
        when(manutencoes.findByVistoriaId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.abrirAPartirDeVistoria(99L, 5L, 7L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(manutencoes, never()).save(any());
    }
}