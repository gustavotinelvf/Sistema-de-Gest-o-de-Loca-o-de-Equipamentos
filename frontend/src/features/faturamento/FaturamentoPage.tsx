import { useEffect, useState } from "react";
import { FaturaDetalhe } from "./components/FaturaDetalhe";
import { faturamentoService } from "./faturamentoService";
import { formatarData, moeda, ROTULOS_STATUS } from "./formato";
import type { Fatura, StatusFatura } from "./types";
import "./faturamento.css";

type Aba = "ABERTAS" | "PAGAS" | "CANCELADAS";

const STATUS_POR_ABA: Record<Aba, StatusFatura[]> = {
  ABERTAS: ["PENDENTE", "VENCIDA"],
  PAGAS: ["PAGA"],
  CANCELADAS: ["CANCELADA"],
};

const ROTULOS_ABA: Record<Aba, string> = {
  ABERTAS: "Em aberto",
  PAGAS: "Pagas",
  CANCELADAS: "Canceladas",
};

const ABAS: Aba[] = ["ABERTAS", "PAGAS", "CANCELADAS"];

export default function FaturamentoPage() {
  const [faturas, setFaturas] = useState<Fatura[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aba, setAba] = useState<Aba>("ABERTAS");
  const [busca, setBusca] = useState("");
  const [selecionadaId, setSelecionadaId] = useState<number | null>(null);

  useEffect(() => {
    let ativo = true;
    faturamentoService
      .listar()
      .then((lista) => {
        if (ativo) setFaturas(lista);
      })
      .catch((e: unknown) => {
        if (ativo)
          setErro(
            e instanceof Error ? e.message : "Ocorreu um erro inesperado",
          );
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, []);

  const termo = busca.trim();
  const visiveis = faturas.filter(
    (f) =>
      STATUS_POR_ABA[aba].includes(f.status) &&
      (termo === "" || String(f.contratoId).includes(termo)),
  );
  const selecionada = faturas.find((f) => f.id === selecionadaId) ?? null;

  function contar(a: Aba) {
    return faturas.filter((f) => STATUS_POR_ABA[a].includes(f.status)).length;
  }

  function aoPagar(paga: Fatura) {
    setFaturas((atual) => atual.map((f) => (f.id === paga.id ? paga : f)));
  }

  return (
    <div className="fat-pagina">
      <header className="fat-cabecalho">
        <span className="fat-sobretitulo">Gestão financeira</span>
        <h1>Faturamento</h1>
        <p>Consulte as faturas dos contratos e registre os pagamentos.</p>
      </header>

      <section className="fat-cartao">
        <div className="fat-abas">
          {ABAS.map((a) => (
            <button
              key={a}
              type="button"
              className={a === aba ? "fat-aba fat-aba-ativa" : "fat-aba"}
              onClick={() => setAba(a)}
            >
              {ROTULOS_ABA[a]} <span className="fat-contador">{contar(a)}</span>
            </button>
          ))}
        </div>

        <input
          className="fat-busca"
          type="text"
          placeholder="Buscar por número do contrato"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />

        {carregando && <p className="fat-vazio">Carregando faturas...</p>}
        {erro && <p className="fat-erro">{erro}</p>}

        {!carregando && !erro && visiveis.length === 0 && (
          <p className="fat-vazio">Nenhuma fatura nesta lista.</p>
        )}

        {visiveis.length > 0 && (
          <div className="fat-rolagem">
            <table className="fat-tabela">
              <thead>
                <tr>
                  <th>Fatura</th>
                  <th>Contrato</th>
                  <th>Emissão</th>
                  <th>Vencimento</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {visiveis.map((f) => (
                  <tr key={f.id}>
                    <td>#{f.id}</td>
                    <td>{f.contratoId}</td>
                    <td>{formatarData(f.dataEmissao)}</td>
                    <td>{formatarData(f.dataVencimento)}</td>
                    <td>
                      {moeda.format(
                        f.valorTotal ?? f.valorLocacao + f.valorMulta,
                      )}
                    </td>
                    <td>
                      <span
                        className={`fat-status fat-status-${f.status.toLowerCase()}`}
                      >
                        {ROTULOS_STATUS[f.status]}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="fat-link"
                        onClick={() => setSelecionadaId(f.id)}
                      >
                        Ver detalhe
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {selecionada && (
        <FaturaDetalhe
          key={selecionada.id}
          fatura={selecionada}
          onPaga={aoPagar}
          onFechar={() => setSelecionadaId(null)}
        />
      )}
    </div>
  );
}
