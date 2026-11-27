import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { definirAoNaoAutorizado } from "../../../shared/api/httpClient";
import {
  limparSessao,
  lerSessao,
  salvarSessao,
  type Sessao,
} from "../../../shared/api/sessao";
import { login } from "../api/authApi";
import { AuthContext } from "./AuthContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(() => lerSessao());

  const sair = useCallback(() => {
    limparSessao();
    setSessao(null);
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const nova = await login(email, senha);
    salvarSessao(nova);
    setSessao(nova);
  }, []);

  // Um 401 da sessão atual encerra a autenticação.
  useEffect(() => {
    definirAoNaoAutorizado(sair);
    return () => definirAoNaoAutorizado(null);
  }, [sair]);

  // Encerra automaticamente a sessão ao atingir sua expiração.
  useEffect(() => {
    if (!sessao) return;

    const verificarExpiracao = () => {
      if (Date.parse(sessao.expiraEm) <= Date.now()) {
        sair();
      }
    };

    const restante = Math.max(0, Date.parse(sessao.expiraEm) - Date.now());
    const timeout = window.setTimeout(verificarExpiracao, restante);

    // Verifica novamente quando o usuário retorna à aba.
    window.addEventListener("focus", verificarExpiracao);
    document.addEventListener("visibilitychange", verificarExpiracao);

    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener("focus", verificarExpiracao);
      document.removeEventListener("visibilitychange", verificarExpiracao);
    };
  }, [sessao, sair]);

  const valor = useMemo(
    () => ({ sessao, entrar, sair }),
    [sessao, entrar, sair],
  );

  return <AuthContext value={valor}>{children}</AuthContext>;
}
