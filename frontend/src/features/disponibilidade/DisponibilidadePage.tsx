import { useState } from 'react'
import './DisponibilidadePage.css'

interface DisponibilidadeModelo {
  modeloId: number
  nome: string
  categoria: string
  valorDiaria: number
  quantidadeDisponivel: number
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function DisponibilidadePage({ resultados }: { resultados?: readonly DisponibilidadeModelo[] }) {
  const [inicio, setInicio] = useState('')
  const [fim, setFim] = useState('')
  const periodoInvalido = Boolean(inicio && fim && fim <= inicio)

  return (
    <main className="disponibilidade">
      <header className="disponibilidade__cabecalho">
        <h1>Disponibilidade de equipamentos</h1>
        <p>Consulte quantas unidades de cada modelo podem ser reservadas no período.</p>
      </header>
      <form aria-labelledby="titulo-filtros-disponibilidade" onSubmit={(event) => event.preventDefault()}>
        <h2 id="titulo-filtros-disponibilidade">Período da consulta</h2>
        <div className="disponibilidade__filtros">
          <label>
            Data de início
            <input type="date" required value={inicio} onChange={(event) => setInicio(event.target.value)} />
          </label>
          <label>
            Data de fim
            <input
              type="date"
              required
              value={fim}
              aria-invalid={periodoInvalido}
              aria-describedby={periodoInvalido ? 'disponibilidade-periodo-erro' : undefined}
              onChange={(event) => setFim(event.target.value)}
            />
          </label>
          <label>
            Categoria (opcional)
            <input type="text" name="categoria" maxLength={100} />
          </label>
          <button type="submit" disabled>Consultar disponibilidade</button>
        </div>
        {periodoInvalido && (
          <p id="disponibilidade-periodo-erro" role="alert">
            A data de fim deve ser posterior à data de início.
          </p>
        )}
      </form>

      <section className="disponibilidade__resultado" aria-labelledby="titulo-resultado-disponibilidade">
        <h2 id="titulo-resultado-disponibilidade">Resultado da consulta</h2>
        <div className="disponibilidade__tabela">
          <table aria-labelledby="titulo-resultado-disponibilidade">
            <thead>
              <tr>
                <th scope="col">Modelo</th>
                <th scope="col">Categoria</th>
                <th scope="col">Diária</th>
                <th scope="col" className="disponibilidade__quantidade">Unidades disponíveis</th>
              </tr>
            </thead>
            <tbody>
              {resultados?.length ? (
                resultados.map((modelo) => (
                  <tr key={modelo.modeloId}>
                    <th scope="row">{modelo.nome}</th>
                    <td>{modelo.categoria}</td>
                    <td>{moeda.format(modelo.valorDiaria)}</td>
                    <td className="disponibilidade__quantidade">
                      <strong className="disponibilidade__total">{modelo.quantidadeDisponivel}</strong>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4} className="disponibilidade__vazio">
                    <span role="status">
                      {resultados === undefined
                        ? 'A consulta está indisponível no momento.'
                        : 'Nenhum modelo disponível para o período e os filtros selecionados.'}
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
