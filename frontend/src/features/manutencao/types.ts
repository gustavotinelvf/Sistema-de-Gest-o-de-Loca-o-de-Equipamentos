export type StatusManutencao = "AGUARDANDO" | "EM_ANDAMENTO" | "CONCLUIDA";

export interface Manutencao {
  id: number;
  unidadeId: number;
  vistoriaId: number | null;
  usuarioTecnicoId: number;
  dataInicio: string;
  dataFim: string | null;
  descricaoProblema: string;
  custoReparo: number | null;
  status: StatusManutencao;
}

export interface IniciarManutencaoRequest {
  numeroSerie: string;
  descricaoProblema: string;
}

export interface ConcluirManutencaoRequest {
  custoReparo: number; // zero ou mais
}
