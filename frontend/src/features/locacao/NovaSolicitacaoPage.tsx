import { useFieldArray, useForm } from 'react-hook-form'
import type { ModeloEquipamento } from '../equipamento/types'
import './NovaSolicitacaoPage.css'

interface CamposSolicitacao {
  dataInicioDesejada: string
  dataFimDesejada: string
  itens: { modeloId: string; quantidade: number }[]
}

interface Props {
  modelos: readonly ModeloEquipamento[]
}

export function NovaSolicitacaoPage({ modelos }: Props) {
  const hoje = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo' }).format(new Date())
  const modelosAtivos = modelos.filter((modelo) => modelo.ativo)
  const { control, register, formState: { errors } } = useForm<CamposSolicitacao>({
    mode: 'onTouched',
    defaultValues: { dataInicioDesejada: '', dataFimDesejada: '', itens: [] },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'itens' })

  return (
    <main className="solicitacao">
      <h1>Nova solicitação de locação</h1>
      <form onSubmit={(event) => event.preventDefault()}>
        <fieldset>
          <legend>Período desejado</legend>
          <label>
            Data de início
            <input type="date" min={hoje} required aria-invalid={!!errors.dataInicioDesejada}
              {...register('dataInicioDesejada', {
                required: 'Informe a data de início.',
                validate: (inicio) => inicio >= hoje || 'A data de início deve ser hoje ou posterior.',
                deps: ['dataFimDesejada'],
              })}
            />
            {errors.dataInicioDesejada && <span role="alert">{errors.dataInicioDesejada.message}</span>}
          </label>
          <label>
            Data de fim
            <input type="date" min={hoje} required aria-invalid={!!errors.dataFimDesejada}
              {...register('dataFimDesejada', {
                required: 'Informe a data de fim.',
                validate: (fim, { dataInicioDesejada }) =>
                  !dataInicioDesejada || fim > dataInicioDesejada || 'A data de fim deve ser posterior ao início.',
              })}
            />
            {errors.dataFimDesejada && <span role="alert">{errors.dataFimDesejada.message}</span>}
          </label>
        </fieldset>

        <fieldset>
          <legend>Itens da solicitação</legend>
          {modelosAtivos.length === 0 && <p>Aguardando a lista de modelos cadastrados.</p>}
          {fields.map((item, indice) => (
            <div className="solicitacao__item" key={item.id}>
              <label>
                Modelo
                <select required aria-invalid={!!errors.itens?.[indice]?.modeloId}
                  {...register(`itens.${indice}.modeloId`, {
                    required: 'Selecione um modelo.',
                    validate: (modeloId, { itens }) =>
                      itens.every((outro, posicao) => posicao === indice || outro.modeloId !== modeloId)
                      || 'Este modelo já está na solicitação.',
                  })}
                >
                  <option value="">Selecione um modelo</option>
                  {modelosAtivos.map((modelo) => (
                    <option key={modelo.id} value={modelo.id}>{modelo.nome}</option>
                  ))}
                </select>
                {errors.itens?.[indice]?.modeloId && <span role="alert">{errors.itens[indice].modeloId.message}</span>}
              </label>
              <label>
                Quantidade
                <input type="number" min={1} max={2147483647} step={1} required
                  aria-invalid={!!errors.itens?.[indice]?.quantidade}
                  {...register(`itens.${indice}.quantidade`, {
                    valueAsNumber: true,
                    validate: (quantidade) =>
                      (Number.isInteger(quantidade) && quantidade >= 1 && quantidade <= 2147483647)
                      || 'Informe uma quantidade inteira maior que zero.',
                  })}
                />
                {errors.itens?.[indice]?.quantidade && <span role="alert">{errors.itens[indice].quantidade.message}</span>}
              </label>
              <button type="button" onClick={() => remove(indice)}>Remover item {indice + 1}</button>
            </div>
          ))}
          <button type="button" disabled={modelosAtivos.length === 0}
            onClick={() => append({ modeloId: '', quantidade: 1 })}
          >
            Adicionar item
          </button>
        </fieldset>
        <p>O envio será habilitado com a seleção de cliente e a integração com a API.</p>
        <button type="submit" disabled>Registrar solicitação</button>
      </form>
    </main>
  )
}
