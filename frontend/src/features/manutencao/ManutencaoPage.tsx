import { useState } from "react";
import { IniciarManutencaoForm } from "./components/IniciarManutencaoForm";
import { ItemManutencao } from "./components/ItemManutencao";
import { manutencaoService } from "./manutencaoService";
import type { Manutencao } from "./types";
import "./manutencao.css";

export default function ManutencaoPage() {
  const [numeroSerie, setNumeroSerie] = useState("");
  const [consultado, setConsultado] = useState<string | null>(null);
  const [itens, setItens] = useState<Manutencao[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function carregar(serie: string) {
    const alvo = serie.trim();

    if (!alvo) {
      setErro("Informe o número de série");
      return;
    }

    setErro(null);
    setCarregando(true);

    try {
      const lista = await manutencaoService.historico(alvo);

      setItens(
        [...lista].sort((a, b) => b.dataInicio.localeCompare(a.dataInicio)),
      );

      setConsultado(alvo);
    } catch (e) {
      setItens([]);
      setConsultado(null);
      setErro(e instanceof Error ? e.message : "Ocorreu um erro inesperado");
    } finally {
      setCarregando(false);
    }
  }

  function aoIniciada(serie: string) {
    setNumeroSerie(serie);
    void carregar(serie);
  }

  return (
    <div className="man-pagina">
      <header className="man-cabecalho">
        <span className="man-sobretitulo">Gestão de manutenção</span>
        <h1>Manutenção</h1>
        <p>Registre manutenções e consulte o histórico por número de série.</p>
      </header>

      <section className="man-cartao">
        <h2>Iniciar manutenção</h2>
        <IniciarManutencaoForm onIniciada={aoIniciada} />
      </section>

      <section className="man-cartao">
        <h2>Histórico por número de série</h2>

        <div className="man-busca">
          <input
            type="text"
            placeholder="Número de série"
            value={numeroSerie}
            onChange={(e) => setNumeroSerie(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && void carregar(numeroSerie)}
          />

          <button
            type="button"
            className="man-botao"
            onClick={() => void carregar(numeroSerie)}
            disabled={carregando}
          >
            {carregando ? "Buscando..." : "Buscar"}
          </button>
        </div>

        {erro && <p className="man-erro">{erro}</p>}

        {consultado !== null && itens.length === 0 && !erro && (
          <p className="man-vazio">
            Nenhuma manutenção encontrada para {consultado}.
          </p>
        )}

        {itens.length > 0 && (
          <ul className="man-lista">
            {itens.map((m) => (
              <ItemManutencao
                key={m.id}
                manutencao={m}
                onConcluida={() => void carregar(consultado ?? numeroSerie)}
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
