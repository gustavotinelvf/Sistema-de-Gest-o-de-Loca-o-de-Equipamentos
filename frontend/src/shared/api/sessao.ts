// Guarda o token do login no sessionStorage.
// O token tem validade curta (2 h), sem renovação automática.

export type Perfil =
  | "GERENTE"
  | "FINANCEIRO"
  | "ATENDENTE"
  | "ESTOQUISTA"
  | "TECNICO";

export interface Sessao {
  token: string;
  perfil: Perfil;
  expiraEm: string;
}

const CHAVE = "ziploc.sessao";

const PERFIS: ReadonlySet<string> = new Set([
  "GERENTE",
  "FINANCEIRO",
  "ATENDENTE",
  "ESTOQUISTA",
  "TECNICO",
]);

let sessaoEmMemoria: Sessao | null = null;
let inicializada = false;

export function sessaoValida(valor: unknown): valor is Sessao {
  if (!valor || typeof valor !== "object") return false;

  const sessao = valor as Partial<Sessao>;

  if (typeof sessao.token !== "string" || !sessao.token.trim()) {
    return false;
  }

  if (typeof sessao.perfil !== "string" || !PERFIS.has(sessao.perfil)) {
    return false;
  }

  if (typeof sessao.expiraEm !== "string") {
    return false;
  }

  const expiracao = Date.parse(sessao.expiraEm);

  return Number.isFinite(expiracao) && expiracao > Date.now();
}

export function lerSessao(): Sessao | null {
  if (!inicializada) {
    inicializada = true;

    try {
      const texto = sessionStorage.getItem(CHAVE);

      if (texto) {
        const valor: unknown = JSON.parse(texto);

        if (sessaoValida(valor)) {
          sessaoEmMemoria = valor;
        } else {
          limparSessao();
        }
      }
    } catch {
      limparSessao();
    }
  }

  if (!sessaoValida(sessaoEmMemoria)) {
    limparSessao();
    return null;
  }

  return sessaoEmMemoria;
}

export function salvarSessao(sessao: Sessao): void {
  if (!sessaoValida(sessao)) {
    throw new Error("Sessão inválida ou expirada.");
  }

  inicializada = true;
  sessaoEmMemoria = { ...sessao };

  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessaoEmMemoria));
  } catch {
    // Sem storage disponível, a sessão permanece em memória.
  }
}

export function limparSessao(): void {
  inicializada = true;
  sessaoEmMemoria = null;

  try {
    sessionStorage.removeItem(CHAVE);
  } catch {
    // A sessão em memória já foi encerrada.
  }
}
