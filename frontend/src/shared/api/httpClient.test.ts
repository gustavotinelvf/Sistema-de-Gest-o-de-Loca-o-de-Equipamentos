import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, definirAoNaoAutorizado, requisitar } from "./httpClient";
import { limparSessao, lerSessao, salvarSessao, type Sessao } from "./sessao";

function criarSessao(token = "token-de-teste"): Sessao {
  return {
    token,
    perfil: "GERENTE",
    expiraEm: new Date(Date.now() + 7200000).toISOString(),
  };
}

function respostaJson(corpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("Cliente HTTP", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    limparSessao();
    definirAoNaoAutorizado(null);
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    definirAoNaoAutorizado(null);
    limparSessao();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("retorna os dados de uma resposta JSON", async () => {
    fetchMock.mockResolvedValue(respostaJson({ id: 1 }));

    const resultado = await requisitar<{ id: number }>("/api/clientes/1");

    expect(resultado).toEqual({ id: 1 });
  });

  it("envia o token JWT nas rotas protegidas", async () => {
    salvarSessao(criarSessao());
    fetchMock.mockResolvedValue(respostaJson([]));

    await requisitar("/api/clientes");

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/clientes"),
      expect.objectContaining({
        method: "GET",
        headers: expect.objectContaining({
          Authorization: "Bearer token-de-teste",
        }),
      }),
    );
  });

  it("não envia Authorization nas rotas públicas", async () => {
    salvarSessao(criarSessao());
    fetchMock.mockResolvedValue(respostaJson({ sucesso: true }));

    await requisitar("/api/auth/login", {
      metodo: "POST",
      publico: true,
      corpo: { email: "teste@teste.com", senha: "senha-teste" },
    });

    const opcoes = fetchMock.mock.calls[0][1];

    expect(opcoes?.headers).not.toHaveProperty("Authorization");
  });

  it("envia o corpo JSON e os cabeçalhos corretamente", async () => {
    const corpo = { email: "teste@teste.com", senha: "senha-teste" };

    fetchMock.mockResolvedValue(respostaJson({ sucesso: true }));

    await requisitar("/api/auth/login", {
      metodo: "POST",
      publico: true,
      corpo,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/auth/login"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(corpo),
        headers: expect.objectContaining({
          Accept: "application/json",
          "Content-Type": "application/json",
        }),
      }),
    );
  });

  it("encerra a sessão ao receber 401 em rota protegida", async () => {
    salvarSessao(criarSessao());

    const sair = vi.fn();
    definirAoNaoAutorizado(sair);

    fetchMock.mockResolvedValue(respostaJson({}, 401));

    await expect(requisitar("/api/clientes")).rejects.toMatchObject({
      status: 401,
    });

    expect(lerSessao()).toBeNull();
    expect(sair).toHaveBeenCalledTimes(1);
  });

  it("preserva a sessão ao receber 401 em rota pública", async () => {
    const sessao = criarSessao();
    salvarSessao(sessao);

    const sair = vi.fn();
    definirAoNaoAutorizado(sair);

    fetchMock.mockResolvedValue(respostaJson({}, 401));

    await expect(
      requisitar("/api/auth/login", { publico: true }),
    ).rejects.toMatchObject({ status: 401 });

    expect(lerSessao()).toEqual(sessao);
    expect(sair).not.toHaveBeenCalled();
  });

  it("preserva a sessão ao receber 403", async () => {
    const sessao = criarSessao();
    salvarSessao(sessao);

    const sair = vi.fn();
    definirAoNaoAutorizado(sair);

    fetchMock.mockResolvedValue(respostaJson({}, 403));

    await expect(requisitar("/api/clientes")).rejects.toMatchObject({
      status: 403,
    });

    expect(lerSessao()).toEqual(sessao);
    expect(sair).not.toHaveBeenCalled();
  });

  it("não encerra uma sessão nova por causa de um 401 antigo", async () => {
    salvarSessao(criarSessao("token-antigo"));

    const sair = vi.fn();
    definirAoNaoAutorizado(sair);

    let responder!: (resposta: Response) => void;

    fetchMock.mockReturnValue(
      new Promise<Response>((resolve) => {
        responder = resolve;
      }),
    );

    const requisicao = requisitar("/api/clientes");
    const novaSessao = criarSessao("token-novo");

    salvarSessao(novaSessao);
    responder(respostaJson({}, 401));

    await expect(requisicao).rejects.toMatchObject({ status: 401 });

    expect(lerSessao()).toEqual(novaSessao);
    expect(sair).not.toHaveBeenCalled();
  });

  it("utiliza a mensagem do ProblemDetail", async () => {
    fetchMock.mockResolvedValue(
      respostaJson(
        {
          detail: "Cliente não encontrado.",
        },
        404,
      ),
    );

    await expect(requisitar("/api/clientes/99")).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "Cliente não encontrado.",
    });
  });

  it("trata respostas de erro sem corpo JSON", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 503 }));

    await expect(requisitar("/api/clientes")).rejects.toMatchObject({
      status: 503,
      message: "Erro 503",
    });
  });

  it("retorna undefined quando recebe 204", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(
      requisitar("/api/clientes/1", { metodo: "DELETE" }),
    ).resolves.toBeUndefined();
  });

  it("propaga erros de conexão", async () => {
    const erro = new TypeError("Failed to fetch");
    fetchMock.mockRejectedValue(erro);

    await expect(requisitar("/api/clientes")).rejects.toBe(erro);
  });

  it("interrompe a requisição após 15 segundos", async () => {
    vi.useFakeTimers();

    fetchMock.mockImplementation((_url, opcoes) => {
      return new Promise<Response>((_resolve, reject) => {
        opcoes?.signal?.addEventListener("abort", () => {
          reject(new DOMException("Abortado", "AbortError"));
        });
      });
    });

    const requisicao = requisitar("/api/clientes");

    const verificacao = expect(requisicao).rejects.toMatchObject({
      name: "ApiError",
      status: 0,
      message: "Tempo limite da requisição excedido.",
    });

    await vi.advanceTimersByTimeAsync(15000);
    await verificacao;
  });

  it("envia o token mesmo quando o sessionStorage está indisponível", async () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("Storage indisponível");
    });

    salvarSessao(criarSessao());
    fetchMock.mockResolvedValue(respostaJson([]));

    await requisitar("/api/clientes");

    expect(fetchMock).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer token-de-teste",
        }),
      }),
    );
  });

  it("mantém o tipo ApiError nas falhas HTTP", async () => {
    fetchMock.mockResolvedValue(respostaJson({}, 500));

    await expect(requisitar("/api/clientes")).rejects.toBeInstanceOf(ApiError);
  });
});
