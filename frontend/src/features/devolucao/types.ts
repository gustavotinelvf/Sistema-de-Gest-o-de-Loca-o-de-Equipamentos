export interface RegistrarDevolucaoRequest {
  itemContratoId: number;
  clienteId: number;
  observacoes?: string; // máximo de 500 caracteres
}

export interface Devolucao {
  id: number;
  itemContratoId: number;
  dataDevolucaoReal: string;
  observacoes: string | null;
  multaAtraso: number | null;
}

export interface ClienteResumo {
  id: number;
  razaoSocial: string;
}

export interface ItemPendente {
  itemContratoId: number;
  descricao: string;
  dataPrevistaDevolucao: string;
}
