import { useState } from 'react'

interface Props {
  status: 'PLANEJADO' | 'ATIVO'
  dataFimPrevista: string
  possuiUnidades: boolean
}

export function AcoesContrato({ status, dataFimPrevista, possuiUnidades }: Props) {
  const [novaDataFim, setNovaDataFim] = useState('')
  const dataInvalida = novaDataFim !== '' && novaDataFim <= dataFimPrevista

  return (
    <section aria-labelledby="titulo-acoes-contrato">
      <h2 id="titulo-acoes-contrato">Ações do contrato</h2>
      <form onSubmit={(event) => event.preventDefault()}>
        {status === 'PLANEJADO' ? (
          <>
            <fieldset>
              <legend>Retirada</legend>
              <label>
                Data da retirada (opcional)
                <input type="date" name="dataRetirada" />
              </label>
              <p>Se o campo ficar em branco, a retirada usa a data de hoje.</p>
              {!possuiUnidades && <p>O contrato precisa ter unidades alocadas para registrar a retirada.</p>}
              <button type="button" disabled>Confirmar retirada</button>
            </fieldset>
            <fieldset>
              <legend>Cancelamento</legend>
              <label>
                Justificativa do cancelamento
                <textarea name="justificativa" rows={3} required />
              </label>
              <button type="button" disabled>Cancelar contrato</button>
            </fieldset>
          </>
        ) : (
          <fieldset>
            <legend>Prorrogação</legend>
            <label>
              Nova data de fim
              <input
                type="date"
                name="novaDataFim"
                required
                value={novaDataFim}
                aria-invalid={dataInvalida}
                aria-describedby={dataInvalida ? 'erro-prorrogacao' : undefined}
                onChange={(event) => setNovaDataFim(event.target.value)}
              />
            </label>
            {dataInvalida && (
              <p id="erro-prorrogacao" role="alert">A nova data deve ser posterior ao fim atual do contrato.</p>
            )}
            <button type="button" disabled>Prorrogar contrato</button>
          </fieldset>
        )}
      </form>
    </section>
  )
}
