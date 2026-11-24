import type {
  ConcluirManutencaoRequest,
  IniciarManutencaoRequest,
  Manutencao,
} from "./types";

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

// mensagens: texto amigável para cada código HTTP, conforme a ação
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
  iniciar: async (dados: IniciarManutencaoRequest) =>
    tratar<Manutencao>(
      await fetch(`${API_URL}/api/manutencoes`, {
        method: "POST",
        headers: cabecalhos(),
        body: JSON.stringify(dados),
      }),
      "iniciar a manutenção",
      {
        404: "Número de série não encontrado",
        409: "Esta unidade já possui uma manutenção em aberto",
      },
    ),

  concluir: async (id: number, dados: ConcluirManutencaoRequest) =>
    tratar<Manutencao>(
      await fetch(`${API_URL}/api/manutencoes/${id}/conclusao`, {
        method: "POST",
        headers: cabecalhos(),
        body: JSON.stringify(dados),
      }),
      "concluir a manutenção",
      {
        404: "Manutenção não encontrada",
        422: "Esta manutenção já foi concluída",
      },
    ),

  historico: async (numeroSerie: string) =>
    tratar<Manutencao[]>(
      await fetch(
        `${API_URL}/api/manutencoes?numeroSerie=${encodeURIComponent(numeroSerie)}`,
        { headers: cabecalhos() },
      ),
      "carregar o histórico",
      { 404: "Número de série não encontrado" },
    ),
};

// ---------- Mock (dados fictícios) ----------
// SN-0001 tem histórico, SN-0002 existe sem histórico; qualquer outro "não existe".
const bancoMock: Record<string, Manutencao[]> = {
  "SN-0001": [
    {
      id: 1,
      unidadeId: 1,
      vistoriaId: null,
      usuarioTecnicoId: 1,
      dataInicio: "2026-10-10T13:00:00Z",
      dataFim: "2026-10-12T17:30:00Z",
      descricaoProblema: "Troca de rolamento",
      custoReparo: 180,
      status: "CONCLUIDA",
    },
  ],
  "SN-0002": [],
};
let proximoId = 100;

function exigirUnidade(numeroSerie: string) {
  if (!Object.hasOwn(bancoMock, numeroSerie)) {
    throw new Error("Número de série não encontrado");
  }
}

const mock = {
  iniciar: async (dados: IniciarManutencaoRequest): Promise<Manutencao> => {
    await pausa();
    exigirUnidade(dados.numeroSerie);
    if (bancoMock[dados.numeroSerie].some((m) => m.status !== "CONCLUIDA")) {
      throw new Error("Esta unidade já possui uma manutenção em aberto");
    }
    const nova: Manutencao = {
      id: proximoId++,
      unidadeId: 1,
      vistoriaId: null,
      usuarioTecnicoId: 1,
      dataInicio: new Date().toISOString(),
      dataFim: null,
      descricaoProblema: dados.descricaoProblema,
      custoReparo: null,
      status: "EM_ANDAMENTO",
    };
    bancoMock[dados.numeroSerie] = [nova, ...bancoMock[dados.numeroSerie]];
    return nova;
  },

  concluir: async (
    id: number,
    dados: ConcluirManutencaoRequest,
  ): Promise<Manutencao> => {
    await pausa();
    for (const lista of Object.values(bancoMock)) {
      const alvo = lista.find((m) => m.id === id);
      if (alvo) {
        if (alvo.status === "CONCLUIDA") {
          throw new Error("Esta manutenção já foi concluída");
        }
        alvo.status = "CONCLUIDA";
        alvo.dataFim = new Date().toISOString();
        alvo.custoReparo = dados.custoReparo;
        return { ...alvo };
      }
    }
    throw new Error("Manutenção não encontrada");
  },

  historico: async (numeroSerie: string): Promise<Manutencao[]> => {
    await pausa();
    exigirUnidade(numeroSerie);
    return bancoMock[numeroSerie].map((m) => ({ ...m }));
  },
};

export const manutencaoService = USAR_MOCK ? mock : real;
