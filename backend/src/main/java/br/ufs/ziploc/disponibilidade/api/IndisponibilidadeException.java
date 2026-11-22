package br.ufs.ziploc.disponibilidade.api;

import java.util.List;
import java.util.stream.Collectors;

public class IndisponibilidadeException extends RuntimeException {
    private final List<Falta> faltas;

    public IndisponibilidadeException(List<Falta> faltas) {
        super("Unidades insuficientes no período: " + faltas.stream().map(f -> "modelo " + f.modeloId() + " (pedido " + f.quantidadePedida() + ", disponível " + f.quantidadeDisponivel() + ")").collect(Collectors.joining(", ")) + ".");
        this.faltas = List.copyOf(faltas);
    }

    public List<Falta> getFaltas() {
        return faltas;
    }

    public record Falta(Long modeloId, int quantidadePedida, int quantidadeDisponivel) {
    }
}
