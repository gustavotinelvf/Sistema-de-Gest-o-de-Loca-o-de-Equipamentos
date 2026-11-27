import { Link, useParams } from 'react-router-dom'
import { AcoesContrato } from './AcoesContrato'
import './NovaSolicitacaoPage.css'

const rotulosStatus = {
  PLANEJADO: 'Planejado',
  ATIVO: 'Ativo',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
}

interface ContratoDetalhe {
  id: number
  solicitacaoId: number | null
  clienteId: number
  dataInicioPrevista: string
  dataFimPrevista: string
  status: keyof typeof rotulosStatus
  valorTotal: number
  percentualMulta: number
  justificativaCancelamento?: string | null
  itens: readonly { id: number; unidadeId: number; valorDiariaContratada?: number }[]
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const data = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' })
const percentual = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 2 })

export function DetalheContratoPage({ contrato }: { contrato?: ContratoDetalhe }) {
  const { id } = useParams()
  const idValido = /^[1-9]\d*$/.test(id ?? '') && Number.isSafeInteger(Number(id))
  const detalhe = idValido && contrato?.id === Number(id) ? contrato : undefined

  return (
    <main className="solicitacao">
      <h1>{idValido ? `Contrato #${id}` : 'Detalhe do contrato'}</h1>
      {detalhe ? (
        <>
          <dl>
            {detalhe.solicitacaoId !== null && (
              <>
                <dt>Solicitação de origem</dt>
                <dd><Link to={`/solicitacoes/${detalhe.solicitacaoId}`}>#{detalhe.solicitacaoId}</Link></dd>
              </>
            )}
            <dt>Cliente</dt>
            <dd>#{detalhe.clienteId}</dd>
            <dt>Período previsto</dt>
            <dd>{data.format(new Date(detalhe.dataInicioPrevista))} a {data.format(new Date(detalhe.dataFimPrevista))}</dd>
            <dt>Situação</dt>
            <dd>{rotulosStatus[detalhe.status]}</dd>
            <dt>Valor total</dt>
            <dd>{moeda.format(detalhe.valorTotal)}</dd>
            <dt>Multa por atraso</dt>
            <dd>{percentual.format(detalhe.percentualMulta / 100)}</dd>
            {detalhe.justificativaCancelamento && (
              <>
                <dt>Justificativa do cancelamento</dt>
                <dd>{detalhe.justificativaCancelamento}</dd>
              </>
            )}
          </dl>
          <section aria-labelledby="titulo-unidades-contrato">
            <h2 id="titulo-unidades-contrato">Unidades alocadas</h2>
            {detalhe.itens.length > 0 ? (
              <ul>
                {detalhe.itens.map((item) => (
                  <li key={item.id}>
                    Unidade #{item.unidadeId}
                    {item.valorDiariaContratada !== undefined && ` · Diária contratada de ${moeda.format(item.valorDiariaContratada)}`}
                  </li>
                ))}
              </ul>
            ) : <p>Nenhuma unidade alocada neste contrato.</p>}
          </section>
          {(detalhe.status === 'PLANEJADO' || detalhe.status === 'ATIVO') && (
            <AcoesContrato
              key={detalhe.id}
              status={detalhe.status}
              dataFimPrevista={detalhe.dataFimPrevista}
              possuiUnidades={detalhe.itens.length > 0}
            />
          )}
        </>
      ) : (
        <p role="status">
          {idValido ? 'O contrato estará disponível após a integração com a API.' : 'Identificador de contrato inválido.'}
        </p>
      )}
      <p><Link to="/solicitacoes">Voltar para a fila de solicitações</Link></p>
    </main>
  )
}
