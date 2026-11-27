import { useState } from 'react'
import './features/equipamento/pages/ModelosEquipamentoPage.css'

type ModeloEquipamento = {
  id: number
  nome: string
  categoria: string
  descricao: string
  valorDiariaBase: number
  quantidadeMinima: number
  quantidadeMaxima: number | null
  ativo: boolean
}



const modelos: ModeloEquipamento[] = []
const categorias: string[] = []
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function ModelosEquipamentoPage() {
  const [categoria, setCategoria] = useState('')
  const [situacao, setSituacao] = useState('todos')

  const modelosFiltrados = modelos.filter((modelo) => {
    const correspondeCategoria = categoria === '' || modelo.categoria === categoria
    const correspondeSituacao =
      situacao === 'todos' || modelo.ativo === (situacao === 'ativos')

    return correspondeCategoria && correspondeSituacao
  })

  return (
    <main className="catalogo">
      <header className="catalogo__cabecalho">
        <h1>Catálogo de equipamentos</h1>
      </header>

      <section className="catalogo__painel" aria-labelledby="titulo-modelos">
        <h2 id="titulo-modelos">Modelos cadastrados</h2>

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
              {modelosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={4} className="catalogo__vazio">
                    Os modelos cadastrados serão exibidos aqui.
                  </td>
                </tr>
              ) : (
                modelosFiltrados.map((modelo) => (
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
      </section>
    </main>
  )
}

export default ModelosEquipamentoPage
