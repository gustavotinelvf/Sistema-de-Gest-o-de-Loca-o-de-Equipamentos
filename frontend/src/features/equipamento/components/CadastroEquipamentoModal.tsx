import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

interface Props {
  titulo: string
  aoFechar: () => void
  children: ReactNode
}

export function CadastroEquipamentoModal({ titulo, aoFechar, children }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const tituloId = useId()
  const [fechando, setFechando] = useState(false)

  useEffect(() => {
    const elemento = dialogo.current
    elemento?.showModal()
    return () => elemento?.close()
  }, [])

  return (
    <dialog
      ref={dialogo}
      className="catalogo__modal"
      aria-labelledby={tituloId}
      data-fechando={fechando}
      onCancel={(event) => {
        event.preventDefault()
        setFechando(true)
      }}
      onClick={(event) => {
        if (event.target !== event.currentTarget) return
        const { left, right, top, bottom } = event.currentTarget.getBoundingClientRect()
        if (event.clientX < left || event.clientX > right || event.clientY < top || event.clientY > bottom) {
          setFechando(true)
        }
      }}
      onAnimationEnd={(event) => {
        if (event.target === event.currentTarget && event.animationName === 'cadastro-equipamento-sair') {
          event.currentTarget.close()
          aoFechar()
        }
      }}
    >
      <header className="catalogo__modal-cabecalho cabecalho-marca">
        <h2 id={tituloId}>{titulo}</h2>
        <button type="button" aria-label="Fechar cadastro" onClick={() => setFechando(true)}>×</button>
      </header>
      <div className="catalogo__modal-conteudo">
        {children}
        <button className="catalogo__modal-cancelar" type="button" onClick={() => setFechando(true)}>
          Cancelar
        </button>
      </div>
    </dialog>
  )
}
