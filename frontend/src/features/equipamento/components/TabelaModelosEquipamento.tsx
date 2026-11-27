import type { ModeloEquipamento } from '../types'

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface Props {
  modelos: ModeloEquipamento[]
}

export function TabelaModelosEquipamento({ modelos }: Props) {
  return (
    <div className="catalogo__tabela">
      <table aria-label="Modelos de equipamento">
        <thead>
          <tr>
            <th scope="col">Modelo</th>
            <th scope="col">Categoria</th>
            <th scope="col">Valor da diária</th>
            <th scope="col">Situação</th>
          </tr>
        </thead>
        <tbody>
          {modelos.length === 0 ? (
            <tr>
              <td colSpan={4} className="catalogo__vazio">
                Os modelos cadastrados serão exibidos aqui.
              </td>
            </tr>
          ) : (
            modelos.map((modelo) => (
              <tr key={modelo.id}>
                <th scope="row">{modelo.nome}</th>
                <td>{modelo.categoria}</td>
                <td>{moeda.format(modelo.valorDiariaBase)}</td>
                <td>{modelo.ativo ? 'Ativo' : 'Inativo'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
