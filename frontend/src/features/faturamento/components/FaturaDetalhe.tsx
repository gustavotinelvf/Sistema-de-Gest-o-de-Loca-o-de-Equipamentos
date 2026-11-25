import { useState } from "react";
import { faturamentoService } from "../faturamentoService";
import { formatarData, moeda, ROTULOS_STATUS } from "../formato";
import type { Fatura } from "../types";

interface Props {
  fatura: Fatura;
  onPaga: (fatura: Fatura) => void;
  onFechar: () => void;
}

export function FaturaDetalhe({ fatura: f, onPaga, onFechar }: Props) {
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const total = f.valorTotal ?? f.valorLocacao + f.valorMulta;
  const aberta = f.status === "PENDENTE" || f.status === "VENCIDA";

  async function pagar() {
    setErro(null);
    setEnviando(true);
    try {
      const paga = await faturamentoService.registrarPagamento(f.id);
      setConfirmando(false);
      onPaga(paga);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Ocorreu um erro inesperado");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="fat-cartao">
      <div className="fat-detalhe-topo">
        <h2>
          Fatura #{f.id} · Contrato {f.contratoId}
        </h2>
        <span className={`fat-status fat-status-${f.status.toLowerCase()}`}>
          {ROTULOS_STATUS[f.status]}
        </span>
      </div>

      <dl className="fat-dados">
        <div>
          <dt>Valor da locação</dt>
          <dd>{moeda.format(f.valorLocacao)}</dd>
        </div>
        <div>
          <dt>Multa por atraso</dt>
          <dd>{moeda.format(f.valorMulta)}</dd>
        </div>
        <div>
          <dt>Total devido</dt>
          <dd className="fat-total">{moeda.format(total)}</dd>
        </div>
        <div>
          <dt>Emissão</dt>
          <dd>{formatarData(f.dataEmissao)}</dd>
        </div>
        <div>
          <dt>Vencimento</dt>
          <dd>{formatarData(f.dataVencimento)}</dd>
        </div>
        <div>
          <dt>Pagamento</dt>
          <dd>{formatarData(f.dataPagamento)}</dd>
        </div>
      </dl>

      {erro && <p className="fat-erro">{erro}</p>}

      <div className="fat-acoes">
        {aberta && !confirmando && (
          <button
            type="button"
            className="fat-botao"
            onClick={() => setConfirmando(true)}
          >
            Registrar pagamento
          </button>
        )}
        {aberta && confirmando && (
          <>
            <span className="fat-confirmacao">
              Confirmar o pagamento de {moeda.format(total)}?
            </span>
            <button
              type="button"
              className="fat-botao"
              onClick={pagar}
              disabled={enviando}
            >
              {enviando ? "Registrando..." : "Confirmar"}
            </button>
            <button
              type="button"
              className="fat-botao-secundario"
              onClick={() => setConfirmando(false)}
              disabled={enviando}
            >
              Cancelar
            </button>
          </>
        )}
        <button
          type="button"
          className="fat-botao-secundario"
          onClick={onFechar}
        >
          Fechar
        </button>
      </div>
    </section>
  );
}
