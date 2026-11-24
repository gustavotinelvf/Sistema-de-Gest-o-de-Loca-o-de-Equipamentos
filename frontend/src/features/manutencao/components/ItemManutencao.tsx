import { useState } from "react";
import { manutencaoService } from "../manutencaoService";
import type { Manutencao, StatusManutencao } from "../types";

const moeda = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const ROTULOS: Record<StatusManutencao, string> = {
  AGUARDANDO: "Aguardando",
  EM_ANDAMENTO: "Em andamento",
  CONCLUIDA: "Concluída",
};

interface Props {
  manutencao: Manutencao;
  onConcluida: () => void;
}

export function ItemManutencao({ manutencao: m, onConcluida }: Props) {
  const [custo, setCusto] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function concluir() {
    const valor = Number(custo);
    if (custo.trim() === "" || Number.isNaN(valor) || valor < 0) {
      setErro("Informe um custo de reparo válido (zero ou mais)");
      return;
    }
    setErro(null);
    setEnviando(true);
    try {
      await manutencaoService.concluir(m.id, { custoReparo: valor });
      onConcluida();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Ocorreu um erro inesperado");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <li className="man-item">
      <div className="man-item-topo">
        <strong>{m.descricaoProblema}</strong>
        <span className={`man-status man-status-${m.status.toLowerCase()}`}>
          {ROTULOS[m.status]}
        </span>
      </div>
      <span className="man-item-detalhe">
        Início: {new Date(m.dataInicio).toLocaleString("pt-BR")} · Fim:{" "}
        {m.dataFim ? new Date(m.dataFim).toLocaleString("pt-BR") : "em aberto"}
      </span>
      {m.custoReparo !== null && (
        <span className="man-item-detalhe">
          Custo do reparo: {moeda.format(m.custoReparo)}
        </span>
      )}

      {m.status !== "CONCLUIDA" && (
        <div className="man-concluir">
          <input
            type="number"
            min="0"
            step="0.01"
            placeholder="Custo do reparo (R$)"
            value={custo}
            onChange={(e) => setCusto(e.target.value)}
          />
          <button
            type="button"
            className="man-botao"
            onClick={concluir}
            disabled={enviando}
          >
            {enviando ? "Concluindo..." : "Concluir"}
          </button>
        </div>
      )}
      {erro && <span className="man-erro">{erro}</span>}
    </li>
  );
}
