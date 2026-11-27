import { useState } from 'react'
import './DisponibilidadePage.css'

export function DisponibilidadePage() {
  const [inicio, setInicio] = useState('')
  const [fim, setFim] = useState('')
  const periodoInvalido = Boolean(inicio && fim && fim <= inicio)

  return (
    <main className="disponibilidade">
      <h1>Disponibilidade de equipamentos</h1>
      <form onSubmit={(event) => event.preventDefault()}>
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
        {periodoInvalido && (
          <p id="disponibilidade-periodo-erro" role="alert">
            A data de fim deve ser posterior à data de início.
          </p>
        )}
        <label>
          Categoria (opcional)
          <input type="text" name="categoria" maxLength={100} />
        </label>
        <button type="submit" disabled>Consultar disponibilidade</button>
        <p>A consulta será habilitada após a integração com a API.</p>
      </form>
    </main>
  )
}
