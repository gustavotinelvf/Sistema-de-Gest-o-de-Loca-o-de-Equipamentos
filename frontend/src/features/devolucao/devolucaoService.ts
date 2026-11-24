import type {
  ClienteResumo,
  Devolucao,
  ItemPendente,
  RegistrarDevolucaoRequest,
} from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "";
const USAR_MOCK = import.meta.env.VITE_USAR_MOCK === "true";

const pausa = () => new Promise((r) => setTimeout(r, 300));

const CLIENTES_MOCK: ClienteResumo[] = [
  { id: 1, razaoSocial: "Cliente Exemplo A Ltda" },
  { id: 2, razaoSocial: "Cliente Exemplo B S.A." },
];

const ITENS_MOCK: Record<number, ItemPendente[]> = {
  1: [
    {
      itemContratoId: 11,
      descricao: "Betoneira 400 litros · Contrato 101",
      dataPrevistaDevolucao: "2026-11-25T12:00:00Z",
    },
    {
      itemContratoId: 12,
      descricao: "Martelete rompedor · Contrato 101",
      dataPrevistaDevolucao: "2026-11-25T12:00:00Z",
    },
  ],
  2: [
    {
      itemContratoId: 21,
      descricao: "Andaime tubular · Contrato 102",
      dataPrevistaDevolucao: "2026-11-28T12:00:00Z",
    },
  ],
};

async function buscar<T>(caminho: string, mensagemErro: string): Promise<T> {
  const resposta = await fetch(`${API_URL}${caminho}`);
  if (!resposta.ok) {
    throw new Error(`${mensagemErro} (erro ${resposta.status})`);
  }
  return (await resposta.json()) as T;
}

async function registrarReal(
  dados: RegistrarDevolucaoRequest,
): Promise<Devolucao> {
  const resposta = await fetch(`${API_URL}/api/devolucoes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (!resposta.ok) {
    throw new Error(`Erro ${resposta.status} ao registrar a devolução`);
  }
  return (await resposta.json()) as Devolucao;
}

async function registrarMock(
  dados: RegistrarDevolucaoRequest,
): Promise<Devolucao> {
  await pausa();
  return {
    id: Date.now(),
    itemContratoId: dados.itemContratoId,
    dataDevolucaoReal: new Date().toISOString(),
    observacoes: dados.observacoes ?? null,
    multaAtraso: null,
  };
}

export const devolucaoService = {
  registrar: (dados: RegistrarDevolucaoRequest) =>
    USAR_MOCK ? registrarMock(dados) : registrarReal(dados),

  listarClientes: async (): Promise<ClienteResumo[]> => {
    if (USAR_MOCK) {
      await pausa();
      return CLIENTES_MOCK;
    }
    return buscar<ClienteResumo[]>(
      "/api/clientes",
      "Não foi possível carregar os clientes",
    );
  },

  listarItensPendentes: async (clienteId: number): Promise<ItemPendente[]> => {
    if (USAR_MOCK) {
      await pausa();
      return ITENS_MOCK[clienteId] ?? [];
    }
    return buscar<ItemPendente[]>(
      `/api/devolucoes/itens-pendentes?clienteId=${clienteId}`,
      "Não foi possível carregar os itens",
    );
  },
};
