package br.ufs.ziploc.disponibilidade.service;

import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.disponibilidade.api.ItemPedidoDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.disponibilidade.domain.Periodo;
import br.ufs.ziploc.disponibilidade.repository.DisponibilidadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 10);
    private static final BigDecimal DIARIA = new BigDecimal("50.00");
    @Mock
    private DisponibilidadeRepository repository;
    @InjectMocks
    private DisponibilidadeService service;

    @Test
    void consultaNormalizaCategoria() {
        service.consultarDisponibilidade(INICIO, FIM, "  Andaime ");
        verify(repository).contarLivresPorModelo(new Periodo(INICIO, FIM), "Andaime");
        service.consultarDisponibilidade(INICIO, FIM, " ");
        verify(repository).contarLivresPorModelo(new Periodo(INICIO, FIM), null);
    }

    @Test
    void consultaRecusaPeriodoInvalido() {
        assertThatThrownBy(() -> service.consultarDisponibilidade(FIM, INICIO, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void alocaAsPrimeirasUnidadesLivresDepoisDeTravar() {
        when(repository.buscarUnidadesLivres(anyCollection(), eq(new Periodo(INICIO, FIM)))).thenReturn(List.of(unidade(1, 10), unidade(2, 10), unidade(3, 10), unidade(4, 20)));
        List<UnidadeDTO> alocadas = service.alocarUnidades(List.of(new ItemPedidoDTO(10L, 1), new ItemPedidoDTO(20L, 1), new ItemPedidoDTO(10L, 1)), INICIO, FIM);
        assertThat(alocadas).extracting(UnidadeDTO::unidadeId).containsExactly(1L, 2L, 4L);
        InOrder ordem = inOrder(repository);
        ordem.verify(repository).travarUnidadesDosModelos(Set.of(10L, 20L));
        ordem.verify(repository).buscarUnidadesLivres(anyCollection(), any());
    }

    @Test
    void lancaConflitoComTodasAsFaltas() {
        when(repository.buscarUnidadesLivres(anyCollection(), any())).thenReturn(List.of(unidade(1, 10)));
        assertThatThrownBy(() -> service.alocarUnidades(List.of(new ItemPedidoDTO(10L, 2), new ItemPedidoDTO(20L, 1)), INICIO, FIM))
                .isInstanceOfSatisfying(IndisponibilidadeException.class, e -> assertThat(e.getFaltas()).containsExactly(new IndisponibilidadeException.Falta(10L, 2, 1), new IndisponibilidadeException.Falta(20L, 1, 0)));
    }

    @Test
    void recusaPedidoInvalido() {
        assertThatThrownBy(() -> service.alocarUnidades(List.of(), INICIO, FIM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alocarUnidades(List.of(new ItemPedidoDTO(10L, 0)), INICIO, FIM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.alocarUnidades(List.of(new ItemPedidoDTO(null, 1)), INICIO, FIM)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    private static UnidadeDTO unidade(long id, long modeloId) {
        return new UnidadeDTO(id, modeloId, DIARIA);
    }
}
