import { lerSessao, limparSessao } from './sessao'

// Só o endereço-base fica aqui. A URL de cada endpoint fica na pasta api/ do módulo.
const URL_BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, mensagem: string) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
  }
}

let aoNaoAutorizado: (() => void) | null = null

/** Registra o que fazer quando uma rota protegida responder 401 (token ausente ou expirado). */
export function definirAoNaoAutorizado(acao: (() => void) | null): void {
  aoNaoAutorizado = acao
}

interface Opcoes {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  corpo?: unknown
  /** Rota pública (ex.: login): não envia token e um 401 não derruba a sessão. */
  publico?: boolean
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const { metodo = 'GET', corpo, publico = false } = opcoes
  const cabecalhos: Record<string, string> = { Accept: 'application/json' }
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'

  if (!publico) {
    const sessao = lerSessao()
    if (sessao) cabecalhos['Authorization'] = `Bearer ${sessao.token}`
  }

  const resposta = await fetch(`${URL_BASE}${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
  })

  if (!resposta.ok) {
    if (resposta.status === 401 && !publico) {
      limparSessao()
      aoNaoAutorizado?.()
    }
    // O backend responde erros em ProblemDetail: a mensagem vem em "detail".
    let detalhe = `Erro ${resposta.status}`
    try {
      const problema = (await resposta.json()) as { detail?: string }
      if (problema.detail) detalhe = problema.detail
    } catch {
      // resposta sem corpo JSON
    }
    throw new ApiError(resposta.status, detalhe)
  }

  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}
