import { useState } from 'react'
import { NovoModeloForm } from '../components/NovoModeloForm'
import { TabelaModelosEquipamento } from '../components/TabelaModelosEquipamento'
import type { ModeloEquipamento } from '../types'
import './ModelosEquipamentoPage.css'

const modelos: ModeloEquipamento[] = []
const categorias: string[] = []

export function ModelosEquipamentoPage() {
  const [categoria, setCategoria] = useState('')
  const [situacao, setSituacao] = useState('todos')
  const [formularioAberto, setFormularioAberto] = useState(false)

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
        <button type="button" aria-expanded={formularioAberto} onClick={() => setFormularioAberto(!formularioAberto)}>
          {formularioAberto ? 'Fechar cadastro' : 'Novo modelo'}
        </button>
      </header>

      {formularioAberto && (
        <section className="catalogo__painel catalogo__cadastro" aria-labelledby="titulo-cadastro">
          <h2 id="titulo-cadastro">Novo modelo</h2>
          <NovoModeloForm />
        </section>
      )}

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

        <TabelaModelosEquipamento modelos={modelosFiltrados} />
      </section>
    </main>
  )
}
