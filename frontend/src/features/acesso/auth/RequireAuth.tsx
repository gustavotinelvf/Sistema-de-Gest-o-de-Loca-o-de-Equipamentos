import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { Perfil } from '../../../shared/api/sessao'
import { useAuth } from './useAuth'

interface Props {
  /** Se informado, só esses perfis entram (o backend continua sendo a barreira real: 403). */
  perfis?: Perfil[]
  children: ReactNode
}

/** Guarda de rota: sem login, vai para /login e volta para a página pedida depois de entrar. */
export function RequireAuth({ perfis, children }: Props) {
  const { sessao } = useAuth()
  const local = useLocation()

  if (!sessao) {
    return <Navigate to="/login" replace state={{ de: local.pathname }} />
  }
  if (perfis && !perfis.includes(sessao.perfil)) {
    return <p role="alert">Acesso negado: seu perfil não pode ver esta página.</p>
  }
  return <>{children}</>
}
