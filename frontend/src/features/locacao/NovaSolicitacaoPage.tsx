import { useEffect, useRef, useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
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
  const dialogo = useRef<HTMLDialogElement>(null)
  const navigate = useNavigate()
  const [fechando, setFechando] = useState(false)
  const hoje = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo' }).format(new Date())
  const modelosAtivos = modelos.filter((modelo) => modelo.ativo)
  const { control, register, formState: { errors } } = useForm<CamposSolicitacao>({
    mode: 'onTouched',
    defaultValues: { dataInicioDesejada: '', dataFimDesejada: '', itens: [] },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'itens' })

  useEffect(() => {
    const elemento = dialogo.current
    elemento?.showModal()
    return () => elemento?.close()
  }, [])

  return (
    <dialog
      ref={dialogo}
      className="solicitacao solicitacao--nova solicitacao__modal"
      aria-labelledby="titulo-nova-solicitacao"
      data-fechando={fechando}
      onCancel={(event) => {
        event.preventDefault()
        setFechando(true)
      }}
      onClick={(event) => {
        if (event.target !== event.currentTarget) return
        const limites = event.currentTarget.getBoundingClientRect()
        if (event.clientX < limites.left || event.clientX > limites.right ||
            event.clientY < limites.top || event.clientY > limites.bottom) {
          setFechando(true)
        }
      }}
      onAnimationEnd={(event) => {
        if (fechando && event.target === event.currentTarget && event.animationName === 'solicitacao-modal-sair') {
          event.currentTarget.close()
          void navigate('/solicitacoes', { replace: true })
        }
      }}
    >
      <header className="solicitacao__cabecalho cabecalho-marca">
        <div>
          <h2 id="titulo-nova-solicitacao">Nova solicitação de locação</h2>
          <p>Informe o período desejado e os equipamentos que o cliente precisa.</p>
        </div>
        <button type="button" className="solicitacao__fechar" aria-label="Fechar nova solicitação" onClick={() => setFechando(true)}>×</button>
      </header>
      <form onSubmit={(event) => event.preventDefault()}>
        <fieldset className="solicitacao__periodo">
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
          {fields.length === 0 && (
            <p className="solicitacao__itens-vazio" role="status">
              {modelosAtivos.length === 0
                ? 'Aguardando a lista de modelos cadastrados.'
                : 'Nenhum item adicionado.'}
            </p>
          )}
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
        <div className="solicitacao__rodape">
          <button type="button" className="solicitacao__cancelar" onClick={() => setFechando(true)}>Cancelar</button>
          <button type="submit" disabled>Registrar solicitação</button>
        </div>
      </form>
    </dialog>
  )
}
