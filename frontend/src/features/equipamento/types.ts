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
