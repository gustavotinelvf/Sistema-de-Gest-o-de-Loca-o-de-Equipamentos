package br.ufs.ziploc.disponibilidade.service;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeApi;
import br.ufs.ziploc.disponibilidade.api.DisponibilidadeDTO;
import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.disponibilidade.api.ItemPedidoDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.disponibilidade.domain.Periodo;
import br.ufs.ziploc.disponibilidade.repository.DisponibilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService implements DisponibilidadeApi {
    private final DisponibilidadeRepository disponibilidadeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadeDTO> consultarDisponibilidade(LocalDate inicio, LocalDate fim, String categoria) {
        String filtro = categoria == null || categoria.isBlank() ? null : categoria.strip();
        return disponibilidadeRepository.contarLivresPorModelo(new Periodo(inicio, fim), filtro);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UnidadeDTO> alocarUnidades(List<ItemPedidoDTO> itens, LocalDate inicio, LocalDate fim) {
        Periodo periodo = new Periodo(inicio, fim);
        Map<Long, Integer> pedido = agruparPorModelo(itens);
        disponibilidadeRepository.travarUnidadesDosModelos(pedido.keySet());
        Map<Long, List<UnidadeDTO>> livres = disponibilidadeRepository.buscarUnidadesLivres(pedido.keySet(), periodo).stream().collect(Collectors.groupingBy(UnidadeDTO::modeloId));
        List<UnidadeDTO> alocadas = new ArrayList<>();
        List<IndisponibilidadeException.Falta> faltas = new ArrayList<>();
        pedido.forEach((modeloId, quantidade) -> {
            List<UnidadeDTO> doModelo = livres.getOrDefault(modeloId, List.of());
            if (doModelo.size() < quantidade) faltas.add(new IndisponibilidadeException.Falta(modeloId, quantidade, doModelo.size()));
            else alocadas.addAll(doModelo.subList(0, quantidade));
        });
        if (!faltas.isEmpty()) throw new IndisponibilidadeException(faltas);
        return alocadas;
    }

    private static Map<Long, Integer> agruparPorModelo(List<ItemPedidoDTO> itens) {
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Informe ao menos um item para alocar.");
        Map<Long, Integer> pedido = new LinkedHashMap<>();
        for (ItemPedidoDTO item : itens) {
            if (item.modeloId() == null) throw new IllegalArgumentException("O modelo de equipamento é obrigatório.");
            if (item.quantidade() <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            pedido.merge(item.modeloId(), item.quantidade(), Integer::sum);
        }
        return pedido;
    }
}
