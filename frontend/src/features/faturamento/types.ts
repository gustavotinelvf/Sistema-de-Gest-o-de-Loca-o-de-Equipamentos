export type StatusFatura = "PENDENTE" | "VENCIDA" | "PAGA" | "CANCELADA";

export interface Fatura {
  id: number;
  contratoId: number;
  valorLocacao: number;
  valorMulta: number;
  valorTotal: number | null;
  status: StatusFatura;
  dataEmissao: string; // "AAAA-MM-DD"
  dataVencimento: string; // "AAAA-MM-DD"
  dataPagamento: string | null; // "AAAA-MM-DD"
}
