import type { ModeloEquipamento } from '../types'

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface Props {
  modelos: readonly ModeloEquipamento[]
  mensagemVazia: string
  aoEditar: (modelo: ModeloEquipamento) => void
}

export function TabelaModelosEquipamento({ modelos, mensagemVazia, aoEditar }: Props) {
  return (
    <div className="catalogo__tabela">
      <table aria-label="Modelos de equipamento">
        <thead>
          <tr>
            <th scope="col">Modelo</th>
            <th scope="col">Categoria</th>
            <th scope="col">Valor da diária</th>
            <th scope="col">Situação</th>
            <th scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          {modelos.length === 0 ? (
            <tr>
              <td colSpan={5} className="catalogo__vazio">
                <span role="status">{mensagemVazia}</span>
              </td>
            </tr>
          ) : (
            modelos.map((modelo) => (
              <tr key={modelo.id}>
                <th scope="row">{modelo.nome}</th>
                <td>{modelo.categoria}</td>
                <td>{moeda.format(modelo.valorDiariaBase)}</td>
                <td>
                  <span className="catalogo__situacao" data-ativo={modelo.ativo}>
                    {modelo.ativo ? 'Ativo' : 'Inativo'}
                  </span>
                </td>
                <td>
                  <button type="button" aria-label={`Editar ${modelo.nome}`} onClick={() => aoEditar(modelo)}>
                    Editar
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
