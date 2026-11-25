package br.ufs.ziploc.disponibilidade.api;

import java.time.LocalDate;
import java.util.List;

public interface DisponibilidadeApi {
    List<DisponibilidadeDTO> consultarDisponibilidade(LocalDate inicio, LocalDate fim, String categoria);

    List<UnidadeDTO> alocarUnidades(List<ItemPedidoDTO> itens, LocalDate inicio, LocalDate fim);
}
