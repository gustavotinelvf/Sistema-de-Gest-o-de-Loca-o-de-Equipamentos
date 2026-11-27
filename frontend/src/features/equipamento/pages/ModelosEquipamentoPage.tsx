import { useState } from 'react'
import { NovaUnidadeForm } from '../components/NovaUnidadeForm'
import { NovoModeloForm } from '../components/NovoModeloForm'
import { TabelaModelosEquipamento } from '../components/TabelaModelosEquipamento'
import type { ModeloEquipamento } from '../types'
import './ModelosEquipamentoPage.css'

const modelos: ModeloEquipamento[] = []
const categorias: string[] = []

export function ModelosEquipamentoPage() {
  const [categoria, setCategoria] = useState('')
  const [situacao, setSituacao] = useState('todos')
  const [cadastro, setCadastro] = useState<'modelo' | 'unidade' | null>(null)

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
        <div className="catalogo__acoes">
          <button type="button" aria-expanded={cadastro === 'modelo'} onClick={() => setCadastro(cadastro === 'modelo' ? null : 'modelo')}>
            {cadastro === 'modelo' ? 'Fechar cadastro de modelo' : 'Novo modelo'}
          </button>
          <button type="button" aria-expanded={cadastro === 'unidade'} onClick={() => setCadastro(cadastro === 'unidade' ? null : 'unidade')}>
            {cadastro === 'unidade' ? 'Fechar cadastro de unidade' : 'Nova unidade'}
          </button>
        </div>
      </header>

      {cadastro === 'modelo' && (
        <section className="catalogo__painel catalogo__cadastro" aria-labelledby="titulo-cadastro">
          <h2 id="titulo-cadastro">Novo modelo</h2>
          <NovoModeloForm />
        </section>
      )}

      {cadastro === 'unidade' && (
        <section className="catalogo__painel catalogo__cadastro" aria-labelledby="titulo-cadastro-unidade">
          <h2 id="titulo-cadastro-unidade">Nova unidade física</h2>
          <NovaUnidadeForm modelos={modelos} />
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
