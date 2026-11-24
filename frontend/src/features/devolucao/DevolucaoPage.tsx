import { useState } from "react";
import { RegistrarDevolucaoForm } from "./components/RegistrarDevolucaoForm";
import type { Devolucao } from "./types";
import "./devolucao.css";

const moeda = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export default function DevolucaoPage() {
  const [registradas, setRegistradas] = useState<Devolucao[]>([]);

  return (
    <div className="dev-pagina">
      <header className="dev-cabecalho">
        <span className="dev-sobretitulo">Gestão de devoluções</span>
        <h1>Devoluções</h1>
        <p>
          Registre a devolução de equipamentos e acompanhe a multa por atraso.
        </p>
      </header>

      <section className="dev-cartao">
        <h2>Registrar devolução</h2>
        <RegistrarDevolucaoForm
          onRegistrada={(d) => setRegistradas((atual) => [d, ...atual])}
        />
      </section>

      <section className="dev-cartao">
        <h2>
          Registradas nesta sessão{" "}
          <span className="dev-contador">{registradas.length}</span>
        </h2>
        {registradas.length === 0 ? (
          <p className="dev-vazio">Nenhuma devolução registrada ainda.</p>
        ) : (
          <ul className="dev-lista">
            {registradas.map((d) => (
              <li key={d.id} className="dev-item">
                <div className="dev-item-texto">
                  <strong>Item de contrato #{d.itemContratoId}</strong>
                  <span>
                    {new Date(d.dataDevolucaoReal).toLocaleString("pt-BR")}
                  </span>
                  {d.observacoes && <span>{d.observacoes}</span>}
                </div>
                <span className={d.multaAtraso ? "dev-multa" : "dev-sem-multa"}>
                  {d.multaAtraso ? moeda.format(d.multaAtraso) : "Sem multa"}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
