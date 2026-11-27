import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import { DisponibilidadePage } from './features/disponibilidade/DisponibilidadePage'
import { ModelosEquipamentoPage } from './features/equipamento/pages/ModelosEquipamentoPage'
import { DetalheSolicitacaoPage } from './features/locacao/DetalheSolicitacaoPage'
import { FilaSolicitacoesPage } from './features/locacao/FilaSolicitacoesPage'
import { NovaSolicitacaoPage } from './features/locacao/NovaSolicitacaoPage'

export default function App() {
  return (
    <BrowserRouter>
      <nav className="navegacao-principal" aria-label="Módulos">
        <NavLink to="/" end>Catálogo</NavLink>
        <NavLink to="/disponibilidade">Disponibilidade</NavLink>
        <NavLink to="/solicitacoes" end>Fila de solicitações</NavLink>
        <NavLink to="/solicitacoes/nova">Nova solicitação</NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<ModelosEquipamentoPage />} />
        <Route path="/disponibilidade" element={<DisponibilidadePage />} />
        <Route path="/solicitacoes" element={<FilaSolicitacoesPage solicitacoes={[]} />} />
        <Route path="/solicitacoes/nova" element={<NovaSolicitacaoPage modelos={[]} />} />
        <Route path="/solicitacoes/:id" element={<DetalheSolicitacaoPage />} />
      </Routes>
    </BrowserRouter>
  )
}
