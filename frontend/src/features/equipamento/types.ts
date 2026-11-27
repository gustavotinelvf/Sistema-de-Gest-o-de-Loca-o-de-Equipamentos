import { createElement } from 'react'

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

export function ModelosEquipamentoPage() {
    return createElement(
        'main',
        null,
        createElement('h1', null, 'Catálogo de Equipamento'),
    )
}
