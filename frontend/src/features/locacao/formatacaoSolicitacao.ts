export const rotulosStatus = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  REJEITADA: 'Rejeitada',
  CANCELADA: 'Cancelada',
}

const data = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' })

export function formatarDataSolicitacao(valor: string) {
  return data.format(new Date(valor))
}
