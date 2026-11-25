import type { StatusFatura } from "./types";

export const moeda = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

// Datas do back vêm como "AAAA-MM-DD". Não usar new Date: no Brasil mostraria um dia antes.
export function formatarData(iso: string | null): string {
  if (!iso) return "—";
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

export const ROTULOS_STATUS: Record<StatusFatura, string> = {
  PENDENTE: "Pendente",
  VENCIDA: "Vencida",
  PAGA: "Paga",
  CANCELADA: "Cancelada",
};
