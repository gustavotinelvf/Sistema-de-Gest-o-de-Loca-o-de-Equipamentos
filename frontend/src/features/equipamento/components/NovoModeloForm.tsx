import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { NovoModeloEquipamento } from '../types'

const esquema = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(200),
  categoria: z.string().trim().min(1, 'Informe a categoria.').max(100),
  descricao: z.string().trim().min(1, 'Informe as especificações técnicas.'),
  diaria: z.string().trim()
    .regex(/^\d{1,10}(?:[.,]\d{1,2})?$/, 'Informe um valor com até duas casas decimais.')
    .refine((valor) => Number(valor.replace(',', '.')) > 0, 'A diária deve ser maior que zero.'),
  minima: z.string().regex(/^\d+$/, 'Informe um número inteiro a partir de zero.')
    .refine((valor) => Number(valor) <= 2147483647, 'A quantidade excede o limite permitido.'),
  maxima: z.string().regex(/^\d*$/, 'Informe um número inteiro ou deixe em branco.')
    .refine((valor) => Number(valor) <= 2147483647, 'A quantidade excede o limite permitido.'),
}).refine(({ minima, maxima }) => maxima === '' || Number(maxima) > Number(minima), {
  path: ['maxima'],
  message: 'A quantidade máxima deve ser maior que a mínima.',
})

type Campos = z.infer<typeof esquema>

interface Props {
  aoSalvar?: (modelo: NovoModeloEquipamento) => Promise<void>
}

export function NovoModeloForm({ aoSalvar }: Props) {
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<Campos>({
    resolver: zodResolver(esquema),
    mode: 'onTouched',
    defaultValues: { nome: '', categoria: '', descricao: '', diaria: '', minima: '0', maxima: '' },
  })

  const enviar = handleSubmit(async ({ nome, categoria, descricao, diaria, minima, maxima }) => {
    if (!aoSalvar) return

    try {
      await aoSalvar({
        nome,
        categoria,
        descricao,
        valorDiariaBase: Number(diaria.replace(',', '.')),
        quantidadeMinima: Number(minima),
        quantidadeMaxima: maxima === '' ? null : Number(maxima),
      })
    } catch {
      setError('root', { message: 'Não foi possível salvar o modelo. Tente novamente.' })
    }
  })

  return (
    <form className="catalogo__formulario" onSubmit={enviar} noValidate>
      <label>
        Nome
        <input {...register('nome')} maxLength={200} required aria-invalid={!!errors.nome} />
        {errors.nome && <span role="alert">{errors.nome.message}</span>}
      </label>
      <label>
        Categoria
        <input {...register('categoria')} maxLength={100} required aria-invalid={!!errors.categoria} />
        {errors.categoria && <span role="alert">{errors.categoria.message}</span>}
      </label>
      <label>
        Especificações técnicas
        <textarea {...register('descricao')} rows={3} required aria-invalid={!!errors.descricao} />
        {errors.descricao && <span role="alert">{errors.descricao.message}</span>}
      </label>
      <label>
        Valor da diária (R$)
        <input {...register('diaria')} inputMode="decimal" required aria-invalid={!!errors.diaria} />
        {errors.diaria && <span role="alert">{errors.diaria.message}</span>}
      </label>
      <label>
        Quantidade mínima
        <input {...register('minima', { deps: ['maxima'] })} inputMode="numeric" required aria-invalid={!!errors.minima} />
        {errors.minima && <span role="alert">{errors.minima.message}</span>}
      </label>
      <label>
        Quantidade máxima (opcional)
        <input {...register('maxima')} inputMode="numeric" aria-invalid={!!errors.maxima} />
        {errors.maxima && <span role="alert">{errors.maxima.message}</span>}
      </label>
      {!aoSalvar && <p>O salvamento será habilitado após a integração com a API.</p>}
      {errors.root && <p role="alert">{errors.root.message}</p>}
      <button type="submit" disabled={!aoSalvar || isSubmitting}>
        {isSubmitting ? 'Salvando…' : 'Salvar modelo'}
      </button>
    </form>
  )
}
