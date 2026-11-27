import { Link, useParams } from 'react-router-dom'
import { formatarDataSolicitacao, rotulosStatus } from './formatacaoSolicitacao'
import './NovaSolicitacaoPage.css'

interface SolicitacaoDetalhe {
  id: number
  clienteId: number
  dataInicioDesejada: string
  dataFimDesejada: string
  status: keyof typeof rotulosStatus
  justificativa: string | null
  itens: readonly { modeloId: number; quantidade: number }[]
}

export function DetalheSolicitacaoPage({ solicitacao }: { solicitacao?: SolicitacaoDetalhe }) {
  const { id } = useParams()
  const idValido = /^[1-9]\d*$/.test(id ?? '') && Number.isSafeInteger(Number(id))
  const detalhe = idValido && solicitacao?.id === Number(id) ? solicitacao : undefined

  return (
    <main className="solicitacao">
      <h1>{idValido ? `Solicitação #${id}` : 'Detalhe da solicitação'}</h1>
      {!detalhe ? (
        <p role="status">
          {idValido ? 'O detalhe estará disponível após a integração com a API.' : 'Identificador de solicitação inválido.'}
        </p>
      ) : (
        <>
          <dl>
            <dt>Cliente</dt>
            <dd>#{detalhe.clienteId}</dd>
            <dt>Período desejado</dt>
            <dd>{formatarDataSolicitacao(detalhe.dataInicioDesejada)} a {formatarDataSolicitacao(detalhe.dataFimDesejada)}</dd>
            <dt>Situação</dt>
            <dd>{rotulosStatus[detalhe.status]}</dd>
            {detalhe.justificativa && (
              <>
                <dt>Justificativa</dt>
                <dd>{detalhe.justificativa}</dd>
              </>
            )}
          </dl>
          <h2>Itens solicitados</h2>
          <ul>
            {detalhe.itens.map((item) => (
              <li key={item.modeloId}>Modelo #{item.modeloId} · Quantidade {item.quantidade}</li>
            ))}
          </ul>
          {detalhe.status === 'PENDENTE' && (
            <form onSubmit={(event) => event.preventDefault()}>
              <fieldset>
                <legend>Aprovação</legend>
                <label>
                  Multa por atraso (%)
                  <input type="number" name="percentualMulta" min={0} max={100} step={0.01} required />
                </label>
                <label>
                  Condições de devolução
                  <textarea name="condicoesDevolucao" rows={3} required />
                </label>
                <button type="button" disabled>Aprovar</button>
              </fieldset>
              <fieldset>
                <legend>Reprovação ou cancelamento</legend>
                <label>
                  Justificativa
                  <textarea name="justificativa" rows={3} required />
                </label>
                <button type="button" disabled>Reprovar</button>
                <button type="button" disabled>Cancelar solicitação</button>
              </fieldset>
            </form>
          )}
        </>
      )}
      <p><Link to="/solicitacoes">Voltar para a fila</Link></p>
    </main>
  )
}
