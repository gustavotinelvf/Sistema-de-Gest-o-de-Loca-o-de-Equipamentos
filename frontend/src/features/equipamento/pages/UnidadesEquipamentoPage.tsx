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
  return (
    <main className="catalogo">
      <h1>Unidades físicas</h1>
      <section className="catalogo__painel" aria-labelledby="titulo-unidades">
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
              </tr>
            </thead>
            <tbody>
              {unidades?.length ? unidades.map((unidade) => (
                <tr key={unidade.id}>
                  <th scope="row">{unidade.numeroSerie}</th>
                  <td>{unidade.modeloNome}</td>
                  <td>{unidade.categoria}</td>
                  <td>{rotulosStatus[unidade.status]}</td>
                  <td>{unidade.ativo ? 'Ativa' : 'Inativa'}</td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={5} className="catalogo__vazio">
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
