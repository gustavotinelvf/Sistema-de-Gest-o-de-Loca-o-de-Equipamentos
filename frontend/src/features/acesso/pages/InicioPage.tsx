import { useAuth } from '../auth/useAuth'

/** Página inicial PROVISÓRIA (só para provar o guarda de rota e o logout). Cada equipe troca pela sua. */
export function InicioPage() {
  const { sessao, sair } = useAuth()
  return (
    <main style={{ padding: 24 }}>
      <h1>ZipLoc</h1>
      <p>Você está logado como {sessao?.perfil}.</p>
      <button type="button" onClick={sair}>
        Sair
      </button>
    </main>
  )
}
