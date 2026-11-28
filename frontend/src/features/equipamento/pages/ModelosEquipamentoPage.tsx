import { useState } from 'react'
import { CadastroEquipamentoModal } from '../components/CadastroEquipamentoModal'
import { NovaUnidadeForm } from '../components/NovaUnidadeForm'
import { NovoModeloForm } from '../components/NovoModeloForm'
import { TabelaModelosEquipamento } from '../components/TabelaModelosEquipamento'
import type { ModeloEquipamento } from '../types'
import './ModelosEquipamentoPage.css'

type Cadastro = { tipo: 'modelo'; modelo?: ModeloEquipamento } | { tipo: 'unidade' }

export function ModelosEquipamentoPage({ modelos = [] }: { modelos?: readonly ModeloEquipamento[] }) {
  const [categoria, setCategoria] = useState('')
  const [situacao, setSituacao] = useState('todos')
  const [cadastro, setCadastro] = useState<Cadastro | null>(null)
  const categorias = Array.from(new Set(modelos.map((modelo) => modelo.categoria)))
  const criandoModelo = cadastro?.tipo === 'modelo' && !cadastro.modelo
  const temFiltros = categoria !== '' || situacao !== 'todos'

  const modelosFiltrados = modelos.filter((modelo) => {
    const correspondeCategoria = categoria === '' || modelo.categoria === categoria
    const correspondeSituacao =
      situacao === 'todos' || modelo.ativo === (situacao === 'ativos')

    return correspondeCategoria && correspondeSituacao
  })

  return (
    <main className="catalogo">
      <header className="catalogo__cabecalho catalogo__destaque cabecalho-marca">
        <div>
          <h1>Catálogo de equipamentos</h1>
          <p>Gerencie os modelos de locação e cadastre unidades por número de série.</p>
        </div>
        <div className="catalogo__acoes">
          <button type="button" aria-haspopup="dialog" onClick={() => setCadastro({ tipo: 'modelo' })}>
            Novo modelo
          </button>
          <button className="catalogo__botao-secundario" type="button" aria-haspopup="dialog" onClick={() => setCadastro({ tipo: 'unidade' })}>
            Nova unidade
          </button>
        </div>
      </header>

      {criandoModelo && (
        <CadastroEquipamentoModal titulo="Novo modelo" aoFechar={() => setCadastro(null)}>
          <NovoModeloForm />
        </CadastroEquipamentoModal>
      )}

      {cadastro?.tipo === 'modelo' && cadastro.modelo && (
        <section className="catalogo__painel catalogo__cadastro" aria-labelledby="titulo-cadastro">
          <h2 id="titulo-cadastro">Editar {cadastro.modelo.nome}</h2>
          <NovoModeloForm key={cadastro.modelo.id} modelo={cadastro.modelo} />
          <button type="button" onClick={() => setCadastro(null)}>Fechar edição</button>
        </section>
      )}

      {cadastro?.tipo === 'unidade' && (
        <CadastroEquipamentoModal titulo="Nova unidade física" aoFechar={() => setCadastro(null)}>
          <NovaUnidadeForm modelos={modelos} />
        </CadastroEquipamentoModal>
      )}

      <section className="catalogo__painel catalogo__lista" aria-labelledby="titulo-modelos">
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
          {temFiltros && (
            <button className="catalogo__limpar" type="button" onClick={() => {
              setCategoria('')
              setSituacao('todos')
            }}>
              Limpar filtros
            </button>
          )}
        </div>

        <TabelaModelosEquipamento
          modelos={modelosFiltrados}
          mensagemVazia={modelos.length > 0
            ? 'Nenhum modelo corresponde aos filtros selecionados.'
            : 'Os modelos cadastrados serão exibidos aqui.'}
          aoEditar={(modelo) => setCadastro({ tipo: 'modelo', modelo })}
        />
      </section>
    </main>
  )
}
