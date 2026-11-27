export interface ModeloEquipamento {
  id: number
  nome: string
  categoria: string
  descricao: string
  valorDiariaBase: number
  quantidadeMinima: number
  quantidadeMaxima: number | null
  ativo: boolean
}

export type NovoModeloEquipamento = Omit<ModeloEquipamento, 'id' | 'ativo'>

export type StatusUnidadeEquipamento =
  | 'DISPONIVEL'
  | 'LOCADA'
  | 'EM_VISTORIA'
  | 'EM_MANUTENCAO'
  | 'INDISPONIVEL'

export interface UnidadeEquipamento {
  id: number
  modeloId: number
  modeloNome: string
  categoria: string
  numeroSerie: string
  status: StatusUnidadeEquipamento
  ativo: boolean
}
