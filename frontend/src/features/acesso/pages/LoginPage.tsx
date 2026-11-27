import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Navigate, useLocation } from "react-router-dom";
import { z } from "zod";
import { ApiError } from "../../../shared/api/httpClient";
import { useAuth } from "../auth/useAuth";
import "./LoginPage.css";

const esquema = z.object({
  email: z.string().min(1, "Informe o e-mail").email("E-mail inválido"),
  senha: z.string().min(1, "Informe a senha"),
});

type Formulario = z.infer<typeof esquema>;

export function LoginPage() {
  const { sessao, entrar } = useAuth();
  const local = useLocation();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Formulario>({ resolver: zodResolver(esquema) });

  // Já logado (ou acabou de logar): segue para a página que tentou abrir, ou para o início.
  if (sessao) {
    const destino = (local.state as { de?: string } | null)?.de ?? "/";
    return <Navigate to={destino} replace />;
  }

  async function aoEnviar(dados: Formulario) {
    setErroGeral(null);
    try {
      await entrar(dados.email, dados.senha);
    } catch (e) {
      setErroGeral(
        e instanceof ApiError && e.status === 401
          ? "E-mail ou senha incorretos"
          : "Não foi possível entrar. Tente novamente.",
      );
    }
  }

  return (
    <main className="login">
      <h1>Entrar no ZipLoc</h1>
      <form
        onSubmit={handleSubmit(aoEnviar, () => setErroGeral(null))}
        noValidate
      >
        <label>
          E-mail
          <input type="email" autoComplete="username" {...register("email")} />
          {errors.email && <span className="erro">{errors.email.message}</span>}
        </label>
        <label>
          Senha
          <input
            type="password"
            autoComplete="current-password"
            {...register("senha")}
          />
          {errors.senha && <span className="erro">{errors.senha.message}</span>}
        </label>
        {erroGeral && (
          <p role="alert" className="erro">
            {erroGeral}
          </p>
        )}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </main>
  );
}
