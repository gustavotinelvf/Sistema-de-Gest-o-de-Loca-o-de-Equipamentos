import { createContext } from 'react'
import type { Sessao } from '../../../shared/api/sessao'

export interface AuthContextoValor {
  /** null quando ninguém está logado */
  sessao: Sessao | null
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => void
}

export const AuthContext = createContext<AuthContextoValor | null>(null)
