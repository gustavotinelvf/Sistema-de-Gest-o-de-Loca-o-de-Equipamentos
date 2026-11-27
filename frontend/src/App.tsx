import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import { DisponibilidadePage } from './features/disponibilidade/DisponibilidadePage'
import { ModelosEquipamentoPage } from './features/equipamento/pages/ModelosEquipamentoPage'
import { NovaSolicitacaoPage } from './features/locacao/NovaSolicitacaoPage'

export default function App() {
  return (
    <BrowserRouter>
      <nav className="navegacao-principal" aria-label="Módulos">
        <NavLink to="/" end>Catálogo</NavLink>
        <NavLink to="/disponibilidade">Disponibilidade</NavLink>
        <NavLink to="/solicitacoes/nova">Nova solicitação</NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<ModelosEquipamentoPage />} />
        <Route path="/disponibilidade" element={<DisponibilidadePage />} />
        <Route path="/solicitacoes/nova" element={<NovaSolicitacaoPage modelos={[]} />} />
      </Routes>
    </BrowserRouter>
  )
}
