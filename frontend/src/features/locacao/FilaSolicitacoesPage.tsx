import { useState } from 'react'
import { Link } from 'react-router-dom'
import { formatarDataSolicitacao, rotulosStatus } from './formatacaoSolicitacao'
import './NovaSolicitacaoPage.css'
import './FilaSolicitacoesPage.css'

interface SolicitacaoResumo {
  id: number
  clienteId: number
  dataInicioDesejada: string
  dataFimDesejada: string
  valorEstimado: number
  status: keyof typeof rotulosStatus
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function FilaSolicitacoesPage({ solicitacoes }: { solicitacoes: readonly SolicitacaoResumo[] }) {
  const [status, setStatus] = useState('')
  const visiveis = solicitacoes.filter((solicitacao) => !status || solicitacao.status === status)

  return (
    <main className="solicitacao">
      <h1>Fila de solicitações</h1>
      <section className="fila-solicitacoes" aria-label="Solicitações de locação">
        <label>
          Situação
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">Todas</option>
            {Object.entries(rotulosStatus).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>{rotulo}</option>
            ))}
          </select>
        </label>
        <div className="fila-solicitacoes__tabela">
          <table aria-label="Solicitações por situação">
            <thead>
              <tr>
                <th scope="col">Solicitação</th>
                <th scope="col">Cliente</th>
                <th scope="col">Início</th>
                <th scope="col">Fim</th>
                <th scope="col">Valor estimado</th>
                <th scope="col">Situação</th>
              </tr>
            </thead>
            <tbody>
              {visiveis.map((solicitacao) => (
                <tr key={solicitacao.id}>
                  <th scope="row"><Link to={`/solicitacoes/${solicitacao.id}`}>#{solicitacao.id}</Link></th>
                  <td>#{solicitacao.clienteId}</td>
                  <td>{formatarDataSolicitacao(solicitacao.dataInicioDesejada)}</td>
                  <td>{formatarDataSolicitacao(solicitacao.dataFimDesejada)}</td>
                  <td>{moeda.format(solicitacao.valorEstimado)}</td>
                  <td>{rotulosStatus[solicitacao.status]}</td>
                </tr>
              ))}
              {visiveis.length === 0 && (
                <tr>
                  <td colSpan={6}>
                    <span role="status">
                      {solicitacoes.length === 0
                        ? ''
                        : 'Nenhuma solicitação nesta situação.'}
                    </span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}
