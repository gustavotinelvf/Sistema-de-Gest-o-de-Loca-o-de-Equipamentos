import { useState } from 'react'
import { NovaUnidadeForm } from '../components/NovaUnidadeForm'
import type { StatusUnidadeEquipamento, UnidadeEquipamento } from '../types'
import './ModelosEquipamentoPage.css'

const rotulosStatus: Record<StatusUnidadeEquipamento, string> = {
  DISPONIVEL: 'Disponível',
  LOCADA: 'Locada',
  EM_VISTORIA: 'Em vistoria',
  EM_MANUTENCAO: 'Em manutenção',
  INDISPONIVEL: 'Indisponível',
}

export function UnidadesEquipamentoPage({ unidades }: { unidades?: readonly UnidadeEquipamento[] }) {
  const [idEmEdicao, setIdEmEdicao] = useState<number | null>(null)
  const unidadeEmEdicao = unidades?.find((unidade) => unidade.id === idEmEdicao)

  return (
    <main className="catalogo">
      <header className="catalogo__cabecalho catalogo__destaque">
        <div>
          <h1>Unidades físicas</h1>
          <p>Acompanhe cada equipamento pelo número de série e pelo status operacional.</p>
        </div>
      </header>
      {unidadeEmEdicao && (
        <section className="catalogo__painel catalogo__cadastro" aria-labelledby="titulo-edicao-unidade">
          <h2 id="titulo-edicao-unidade">Editar unidade {unidadeEmEdicao.numeroSerie}</h2>
          <NovaUnidadeForm key={unidadeEmEdicao.id} unidade={unidadeEmEdicao} />
          <button type="button" onClick={() => setIdEmEdicao(null)}>Fechar edição</button>
        </section>
      )}
      <section className="catalogo__painel catalogo__lista" aria-labelledby="titulo-unidades">
        <h2 id="titulo-unidades">Unidades cadastradas</h2>
        <div className="catalogo__tabela">
          <table aria-labelledby="titulo-unidades">
            <thead>
              <tr>
                <th scope="col">Número de série</th>
                <th scope="col">Modelo</th>
                <th scope="col">Categoria</th>
                <th scope="col">Status operacional</th>
                <th scope="col">Situação cadastral</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {unidades?.length ? unidades.map((unidade) => (
                <tr key={unidade.id}>
                  <th scope="row" className="catalogo__numero-serie">{unidade.numeroSerie}</th>
                  <td>{unidade.modeloNome}</td>
                  <td>{unidade.categoria}</td>
                  <td>
                    <span className="catalogo__situacao" data-ativo={unidade.status === 'DISPONIVEL'}>
                      {rotulosStatus[unidade.status]}
                    </span>
                  </td>
                  <td>{unidade.ativo ? 'Ativa' : 'Inativa'}</td>
                  <td>
                    <button
                      type="button"
                      aria-label={`Editar unidade ${unidade.numeroSerie}`}
                      onClick={() => setIdEmEdicao(unidade.id)}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={6} className="catalogo__vazio">
                    <span role="status">
                      {unidades === undefined
                        ? 'A lista será exibida após a integração com a API.'
                        : 'Nenhuma unidade cadastrada.'}
                    </span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}
