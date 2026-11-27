import { lerSessao, limparSessao } from "./sessao";

// Só o endereço-base fica aqui. Cada módulo define seus endpoints.
const URL_BASE: string = (
  import.meta.env.VITE_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");
const TIMEOUT_MS = 15000;

export class ApiError extends Error {
  status: number;

  constructor(status: number, mensagem: string) {
    super(mensagem);
    this.name = "ApiError";
    this.status = status;
  }
}

let aoNaoAutorizado: (() => void) | null = null;

export function definirAoNaoAutorizado(acao: (() => void) | null): void {
  aoNaoAutorizado = acao;
}

interface Opcoes {
  metodo?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  corpo?: unknown;
  publico?: boolean;
}

export async function requisitar<T>(
  caminho: string,
  opcoes: Opcoes = {},
): Promise<T> {
  const { metodo = "GET", corpo, publico = false } = opcoes;
  const cabecalhos: Record<string, string> = { Accept: "application/json" };

  if (corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
  }

  const token = publico ? undefined : lerSessao()?.token;

  if (token) {
    cabecalhos["Authorization"] = `Bearer ${token}`;
  }

  const controlador = new AbortController();
  const timeout = setTimeout(() => controlador.abort(), TIMEOUT_MS);

  try {
    const resposta = await fetch(`${URL_BASE}${caminho}`, {
      method: metodo,
      headers: cabecalhos,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
      signal: controlador.signal,
    });

    if (!resposta.ok) {
      if (resposta.status === 401 && !publico) {
        const tokenAtual = lerSessao()?.token;

        // Uma resposta antiga não deve encerrar uma sessão nova.
        if (token === tokenAtual) {
          limparSessao();
          aoNaoAutorizado?.();
        }
      }

      let detalhe = `Erro ${resposta.status}`;

      try {
        const problema = await resposta.json();

        if (typeof problema?.detail === "string" && problema.detail.trim()) {
          detalhe = problema.detail;
        }
      } catch {
        // Resposta sem corpo JSON.
      }

      throw new ApiError(resposta.status, detalhe);
    }

    if (resposta.status === 204) {
      return undefined as T;
    }

    return (await resposta.json()) as T;
  } catch (erro) {
    if (controlador.signal.aborted) {
      throw new ApiError(0, "Tempo limite da requisição excedido.");
    }

    throw erro;
  } finally {
    clearTimeout(timeout);
  }
}
