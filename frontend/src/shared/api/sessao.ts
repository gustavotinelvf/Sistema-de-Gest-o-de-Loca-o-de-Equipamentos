// Guarda o token do login no sessionStorage (some ao fechar a aba).
// O token tem validade curta (2 h, seção 12 do Relatório), então não há renovação.

export type Perfil = 'GERENTE' | 'FINANCEIRO' | 'ATENDENTE' | 'ESTOQUISTA' | 'TECNICO'

export interface Sessao {
  token: string
  perfil: Perfil
  expiraEm: string // ISO 8601, como vem do backend
}

const CHAVE = 'ziploc.sessao'

export function lerSessao(): Sessao | null {
  try {
    const texto = sessionStorage.getItem(CHAVE)
    if (!texto) return null
    const sessao = JSON.parse(texto) as Sessao
    if (new Date(sessao.expiraEm).getTime() <= Date.now()) {
      limparSessao()
      return null
    }
    return sessao
  } catch {
    return null
  }
}

export function salvarSessao(sessao: Sessao): void {
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao))
  } catch {
    // sem storage disponível: a sessão vale só enquanto a página estiver aberta
  }
}

export function limparSessao(): void {
  try {
    sessionStorage.removeItem(CHAVE)
  } catch {
    // ignora
  }
}
