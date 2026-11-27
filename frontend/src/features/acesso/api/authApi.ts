import { requisitar } from '../../../shared/api/httpClient'
import type { Perfil, Sessao } from '../../../shared/api/sessao'

interface TokenResponse {
  token: string
  perfil: Perfil
  expiraEm: string
}

/** POST /api/auth/login (rota pública). */
export async function login(email: string, senha: string): Promise<Sessao> {
  const resposta = await requisitar<TokenResponse>('/api/auth/login', {
    metodo: 'POST',
    corpo: { email, senha },
    publico: true,
  })
  return { token: resposta.token, perfil: resposta.perfil, expiraEm: resposta.expiraEm }
}
