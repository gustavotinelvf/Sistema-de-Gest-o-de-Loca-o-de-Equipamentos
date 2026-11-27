import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { definirAoNaoAutorizado } from '../../../shared/api/httpClient'
import { limparSessao, lerSessao, salvarSessao, type Sessao } from '../../../shared/api/sessao'
import { login } from '../api/authApi'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(() => lerSessao())

  const sair = useCallback(() => {
    limparSessao()
    setSessao(null)
  }, [])

  const entrar = useCallback(async (email: string, senha: string) => {
    const nova = await login(email, senha)
    salvarSessao(nova)
    setSessao(nova)
  }, [])

  // Qualquer 401 numa rota protegida derruba a sessão; o guarda leva ao login.
  useEffect(() => {
    definirAoNaoAutorizado(sair)
    return () => definirAoNaoAutorizado(null)
  }, [sair])

  const valor = useMemo(() => ({ sessao, entrar, sair }), [sessao, entrar, sair])

  return <AuthContext value={valor}>{children}</AuthContext>
}
