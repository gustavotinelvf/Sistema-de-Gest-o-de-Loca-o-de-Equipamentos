import type { Fatura } from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "";
const USAR_MOCK = import.meta.env.VITE_USAR_MOCK === "true";

const pausa = () => new Promise((r) => setTimeout(r, 300));

// ---------- API real ----------
function cabecalhos(): HeadersInit {
  // PROVISÓRIO: quando o login estiver pronto, ler o token de onde ele guardar.
  const token = sessionStorage.getItem("token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function tratar<T>(
  resposta: Response,
  acao: string,
  mensagens: Record<number, string> = {},
): Promise<T> {
  if (resposta.status === 401 || resposta.status === 403) {
    throw new Error(
      "Sem permissão: é preciso estar logado com o perfil correto",
    );
  }
  if (!resposta.ok) {
    throw new Error(
      mensagens[resposta.status] ?? `Erro ${resposta.status} ao ${acao}`,
    );
  }
  return (await resposta.json()) as T;
}

const real = {
  // PROVISÓRIO: o back ainda não tem GET /api/faturas (só o POST de pagamento).
  listar: async () =>
    tratar<Fatura[]>(
      await fetch(`${API_URL}/api/faturas`, { headers: cabecalhos() }),
      "carregar as faturas",
    ),

  registrarPagamento: async (id: number) =>
    tratar<Fatura>(
      await fetch(`${API_URL}/api/faturas/${id}/pagamento`, {
        method: "POST",
        headers: cabecalhos(),
      }),
      "registrar o pagamento",
      {
        404: "Fatura não encontrada",
        409: "Esta fatura não pode mais ser paga",
        422: "Esta fatura não pode mais ser paga",
      },
    ),
};

// ---------- Mock (dados fictícios, mesmo formato do FaturaDto) ----------
const bancoMock: Fatura[] = [
  {
    id: 1,
    contratoId: 101,
    valorLocacao: 850,
    valorMulta: 25.5,
    valorTotal: 875.5,
    status: "PENDENTE",
    dataEmissao: "2026-11-20",
    dataVencimento: "2026-11-30",
    dataPagamento: null,
  },
  {
    id: 2,
    contratoId: 102,
    valorLocacao: 1200,
    valorMulta: 0,
    valorTotal: 1200,
    status: "VENCIDA",
    dataEmissao: "2026-11-01",
    dataVencimento: "2026-11-10",
    dataPagamento: null,
  },
  {
    id: 3,
    contratoId: 103,
    valorLocacao: 600,
    valorMulta: 0,
    valorTotal: 600,
    status: "PAGA",
    dataEmissao: "2026-10-15",
    dataVencimento: "2026-10-25",
    dataPagamento: "2026-10-22",
  },
  {
    id: 4,
    contratoId: 104,
    valorLocacao: 300,
    valorMulta: 0,
    valorTotal: 300,
    status: "CANCELADA",
    dataEmissao: "2026-10-05",
    dataVencimento: "2026-10-15",
    dataPagamento: null,
  },
];

function hojeIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

const mock = {
  listar: async (): Promise<Fatura[]> => {
    await pausa();
    return bancoMock.map((f) => ({ ...f }));
  },

  registrarPagamento: async (id: number): Promise<Fatura> => {
    await pausa();
    const alvo = bancoMock.find((f) => f.id === id);
    if (!alvo) throw new Error("Fatura não encontrada");
    if (alvo.status === "PAGA" || alvo.status === "CANCELADA") {
      throw new Error("Esta fatura não pode mais ser paga");
    }
    alvo.status = "PAGA";
    alvo.dataPagamento = hojeIso();
    return { ...alvo };
  },
};

export const faturamentoService = USAR_MOCK ? mock : real;
