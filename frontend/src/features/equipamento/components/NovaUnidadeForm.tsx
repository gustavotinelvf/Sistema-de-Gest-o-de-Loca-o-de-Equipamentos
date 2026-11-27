import { useState } from 'react'
import type { ModeloEquipamento, UnidadeEquipamento } from '../types'

type Props =
  | { modelos: readonly ModeloEquipamento[]; unidade?: never }
  | { unidade: UnidadeEquipamento; modelos?: never }

export function NovaUnidadeForm({ modelos, unidade }: Props) {
  const [numeroSerie, setNumeroSerie] = useState(unidade?.numeroSerie ?? '')
  const [serieTocada, setSerieTocada] = useState(false)
  const modelosAtivos = modelos?.filter((modelo) => modelo.ativo) ?? []
  const erroSerie = serieTocada && numeroSerie.trim() === ''

  return (
    <form className="catalogo__formulario" onSubmit={(event) => event.preventDefault()}>
      {unidade ? (
        <label>
          Modelo
          <input value={unidade.modeloNome} readOnly />
        </label>
      ) : (
        <>
          <label>
            Modelo
            <select name="modeloId" defaultValue="" required disabled={modelosAtivos.length === 0}>
              <option value="" disabled>Selecione um modelo</option>
              {modelosAtivos.map((modelo) => (
                <option key={modelo.id} value={modelo.id}>{modelo.nome}</option>
              ))}
            </select>
          </label>
          {modelosAtivos.length === 0 && <p>Cadastre um modelo ativo para vincular a unidade.</p>}
        </>
      )}
      <label>
        Número de série
        <input
          name="numeroSerie"
          value={numeroSerie}
          maxLength={50}
          required
          aria-invalid={erroSerie}
          aria-describedby={erroSerie ? 'erro-numero-serie' : undefined}
          onChange={(event) => setNumeroSerie(event.target.value)}
          onBlur={() => setSerieTocada(true)}
        />
        {erroSerie && <span id="erro-numero-serie" role="alert">Informe o número de série.</span>}
      </label>
      <p>O salvamento será habilitado após a integração com a API.</p>
      <button type="submit" disabled>Salvar unidade</button>
    </form>
  )
}
