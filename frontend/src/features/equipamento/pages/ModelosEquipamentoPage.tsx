import { useState } from 'react'
import type { ModeloEquipamento } from '../types'
import './ModelosEquipamentoPage.css'

const modelosExemplo: ModeloEquipamento[] = [
  {
    id: 1,
    nome: 'Betoneira 400 L',
    categoria: 'Construção',
    descricao: 'Betoneira elétrica',
    valorDiariaBase: 120,
    quantidadeMinima: 1,
    quantidadeMaxima: 4,
    ativo: true,
  },
  {
    id: 2,
    nome: 'Furadeira de impacto',
    categoria: 'Ferramentas',
    descricao: 'Furadeira com impacto',
    valorDiariaBase: 45,
    quantidadeMinima: 1,
    quantidadeMaxima: null,
    ativo: false,
  },
]

const categorias = [...new Set(modelosExemplo.map((modelo) => modelo.categoria))]
const moeda = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export function ModelosEquipamentoPage() {
  const [categoria, setCategoria] = useState('')
  const [situacao, setSituacao] = useState('todos')

  const modelosFiltrados = modelosExemplo.filter((modelo) => {
    const correspondeCategoria = categoria === '' || modelo.categoria === categoria
    const correspondeSituacao =
      situacao === 'todos' || modelo.ativo === (situacao === 'ativos')

    return correspondeCategoria && correspondeSituacao
  })

  return (
    <main className="catalogo">
      <h1>Catálogo de equipamentos</h1>

      <div className="catalogo__filtros">
        <label>
          Categoria
          <select value={categoria} onChange={(event) => setCategoria(event.target.value)}>
            <option value="">Todas</option>
            {categorias.map((opcao) => (
              <option key={opcao} value={opcao}>
                {opcao}
              </option>
            ))}
          </select>
        </label>

        <label>
          Situação
          <select value={situacao} onChange={(event) => setSituacao(event.target.value)}>
            <option value="todos">Todos</option>
            <option value="ativos">Ativos</option>
            <option value="inativos">Inativos</option>
          </select>
        </label>
      </div>

      {modelosFiltrados.length === 0 ? (
        <p role="status">Nenhum modelo encontrado para os filtros selecionados.</p>
      ) : (
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
              {modelosFiltrados.map((modelo) => (
                <tr key={modelo.id}>
                  <th scope="row">{modelo.nome}</th>
                  <td>{modelo.categoria}</td>
                  <td>{moeda.format(modelo.valorDiariaBase)}</td>
                  <td>{modelo.ativo ? 'Ativo' : 'Inativo'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
