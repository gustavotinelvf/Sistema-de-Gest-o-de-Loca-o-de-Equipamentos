import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  limparSessao,
  lerSessao,
  salvarSessao,
  type Sessao,
} from "../../../shared/api/sessao";
import { requisitar } from "../../../shared/api/httpClient";
import { InicioPage } from "../pages/InicioPage";
import { LoginPage } from "../pages/LoginPage";
import { AuthProvider } from "./AuthProvider";
import { RequireAuth } from "./RequireAuth";

function criarSessao(alteracoes: Partial<Sessao> = {}): Sessao {
  return {
    token: "token-de-teste",
    perfil: "GERENTE",
    expiraEm: new Date(Date.now() + 7200000).toISOString(),
    ...alteracoes,
  };
}

function renderizar(rota = "/") {
  return render(
    <MemoryRouter initialEntries={[rota]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route
            path="/"
            element={
              <RequireAuth>
                <InicioPage />
              </RequireAuth>
            }
          />

          <Route
            path="/gerencia"
            element={
              <RequireAuth perfis={["GERENTE"]}>
                <h1>Área gerencial</h1>
              </RequireAuth>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

function preencherLogin(email = "gerente@teste.com", senha = "SenhaTeste123") {
  fireEvent.change(screen.getByLabelText("E-mail"), {
    target: { value: email },
  });

  fireEvent.change(screen.getByLabelText("Senha"), {
    target: { value: senha },
  });

  fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
}

describe("Autenticação e proteção de rotas", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    limparSessao();
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    limparSessao();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("redireciona usuários sem sessão para o login", async () => {
    renderizar();

    expect(
      await screen.findByRole("heading", {
        name: "Entrar no ZipLoc",
      }),
    ).toBeInTheDocument();
  });

  it("permite acesso à página inicial com sessão válida", () => {
    salvarSessao(criarSessao());

    renderizar();

    expect(
      screen.getByText("Você está logado como GERENTE."),
    ).toBeInTheDocument();
  });

  it("realiza login e armazena a sessão", async () => {
    const sessao = criarSessao();

    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(sessao), { status: 200 }),
    );

    renderizar("/login");
    preencherLogin();

    expect(
      await screen.findByText("Você está logado como GERENTE."),
    ).toBeInTheDocument();

    expect(lerSessao()).toEqual(sessao);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("retorna à rota originalmente solicitada após o login", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(criarSessao()), { status: 200 }),
    );

    renderizar("/gerencia");
    preencherLogin();

    expect(
      await screen.findByRole("heading", {
        name: "Área gerencial",
      }),
    ).toBeInTheDocument();
  });

  it("rejeita credenciais incorretas", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    renderizar("/login");
    preencherLogin();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "E-mail ou senha incorretos",
    );

    expect(lerSessao()).toBeNull();
  });

  it("informa indisponibilidade do backend", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    renderizar("/login");
    preencherLogin();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível entrar. Tente novamente.",
    );

    expect(lerSessao()).toBeNull();
  });

  it("impede envio do formulário com campos vazios", async () => {
    renderizar("/login");

    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Informe o e-mail")).toBeInTheDocument();

    expect(await screen.findByText("Informe a senha")).toBeInTheDocument();

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejeita endereço de e-mail inválido", async () => {
    renderizar("/login");
    preencherLogin("email-invalido");

    expect(await screen.findByText("E-mail inválido")).toBeInTheDocument();

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejeita resposta de login com sessão inválida", async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify(
          criarSessao({
            expiraEm: "data-invalida",
          }),
        ),
        { status: 200 },
      ),
    );

    renderizar("/login");
    preencherLogin();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível entrar. Tente novamente.",
    );

    expect(lerSessao()).toBeNull();
  });

  it("permite acesso à rota quando o perfil é autorizado", () => {
    salvarSessao(criarSessao());

    renderizar("/gerencia");

    expect(
      screen.getByRole("heading", {
        name: "Área gerencial",
      }),
    ).toBeInTheDocument();
  });

  it("bloqueia acesso à rota quando o perfil não é autorizado", () => {
    salvarSessao(criarSessao({ perfil: "ATENDENTE" }));

    renderizar("/gerencia");

    expect(screen.getByRole("alert")).toHaveTextContent("Acesso negado");

    expect(screen.queryByText("Área gerencial")).not.toBeInTheDocument();

    expect(lerSessao()).not.toBeNull();
  });

  it("encerra a sessão ao realizar logout", async () => {
    salvarSessao(criarSessao());

    renderizar();

    fireEvent.click(screen.getByRole("button", { name: "Sair" }));

    expect(
      await screen.findByRole("heading", {
        name: "Entrar no ZipLoc",
      }),
    ).toBeInTheDocument();

    expect(lerSessao()).toBeNull();
  });

  it("encerra a sessão após 401 de uma rota protegida", async () => {
    salvarSessao(criarSessao());

    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    renderizar();

    await act(async () => {
      await expect(requisitar("/api/clientes")).rejects.toMatchObject({
        status: 401,
      });
    });

    expect(
      await screen.findByRole("heading", {
        name: "Entrar no ZipLoc",
      }),
    ).toBeInTheDocument();

    expect(lerSessao()).toBeNull();
  });

  it("preserva a autenticação após resposta 403", async () => {
    salvarSessao(criarSessao());

    fetchMock.mockResolvedValue(new Response(null, { status: 403 }));

    renderizar();

    await act(async () => {
      await expect(requisitar("/api/clientes")).rejects.toMatchObject({
        status: 403,
      });
    });

    expect(
      screen.getByText("Você está logado como GERENTE."),
    ).toBeInTheDocument();
  });

  it("encerra automaticamente a sessão expirada", async () => {
    vi.useFakeTimers();

    salvarSessao(
      criarSessao({
        expiraEm: new Date(Date.now() + 1000).toISOString(),
      }),
    );

    renderizar();

    expect(
      screen.getByText("Você está logado como GERENTE."),
    ).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    expect(
      screen.getByRole("heading", {
        name: "Entrar no ZipLoc",
      }),
    ).toBeInTheDocument();

    expect(lerSessao()).toBeNull();
  });

  it("desabilita o botão enquanto o login está em andamento", async () => {
    let responder!: (resposta: Response) => void;

    fetchMock.mockReturnValue(
      new Promise<Response>((resolve) => {
        responder = resolve;
      }),
    );

    renderizar("/login");
    preencherLogin();

    await waitFor(() => {
      expect(
        screen.getByRole("button", {
          name: "Entrando...",
        }),
      ).toBeDisabled();
    });

    await act(async () => {
      responder(new Response(JSON.stringify(criarSessao()), { status: 200 }));
    });

    expect(
      await screen.findByText("Você está logado como GERENTE."),
    ).toBeInTheDocument();
  });

  it("remove o erro anterior ao enviar um formulário inválido", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    renderizar("/login");
    preencherLogin();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "E-mail ou senha incorretos",
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled();
    });

    preencherLogin("", "");

    expect(await screen.findByText("Informe o e-mail")).toBeInTheDocument();

    expect(await screen.findByText("Informe a senha")).toBeInTheDocument();

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(lerSessao()).toBeNull();
  });
});
