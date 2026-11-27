import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Sessao } from "./sessao";

const CHAVE = "ziploc.sessao";

function criarSessao(): Sessao {
  return {
    token: "token-de-teste",
    perfil: "GERENTE",
    expiraEm: new Date(Date.now() + 7200000).toISOString(),
  };
}

describe("Gerenciamento de sessão", () => {
  beforeEach(() => {
    vi.resetModules();
    sessionStorage.clear();
  });

  it("retorna null quando não existe sessão", async () => {
    const { lerSessao } = await import("./sessao");

    expect(lerSessao()).toBeNull();
  });

  it("salva e recupera uma sessão válida", async () => {
    const { salvarSessao, lerSessao } = await import("./sessao");
    const sessao = criarSessao();

    salvarSessao(sessao);

    expect(lerSessao()).toEqual(sessao);
    expect(JSON.parse(sessionStorage.getItem(CHAVE)!)).toEqual(sessao);
  });

  it("recupera uma sessão após recarregar o módulo", async () => {
    const sessao = criarSessao();

    sessionStorage.setItem(CHAVE, JSON.stringify(sessao));

    const { lerSessao } = await import("./sessao");

    expect(lerSessao()).toEqual(sessao);
  });

  it("remove uma sessão expirada", async () => {
    sessionStorage.setItem(
      CHAVE,
      JSON.stringify({
        ...criarSessao(),
        expiraEm: new Date(Date.now() - 1000).toISOString(),
      }),
    );

    const { lerSessao } = await import("./sessao");

    expect(lerSessao()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it("remove uma sessão com JSON corrompido", async () => {
    sessionStorage.setItem(CHAVE, "{json-invalido");

    const { lerSessao } = await import("./sessao");

    expect(lerSessao()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it.each([
    { token: "" },
    { token: "   " },
    { token: null },
    { perfil: "INVALIDO" },
    { expiraEm: "data-invalida" },
    { expiraEm: null },
  ])("rejeita sessão com dados inválidos: %j", async (alteracao) => {
    sessionStorage.setItem(
      CHAVE,
      JSON.stringify({
        ...criarSessao(),
        ...alteracao,
      }),
    );

    const { lerSessao } = await import("./sessao");

    expect(lerSessao()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it("rejeita o armazenamento de uma sessão inválida", async () => {
    const { salvarSessao } = await import("./sessao");

    expect(() =>
      salvarSessao({
        ...criarSessao(),
        token: "",
      }),
    ).toThrow("Sessão inválida ou expirada.");
  });

  it("encerra a sessão e remove os dados armazenados", async () => {
    const { salvarSessao, limparSessao, lerSessao } = await import("./sessao");

    salvarSessao(criarSessao());
    limparSessao();

    expect(lerSessao()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it("mantém a sessão em memória quando o storage falha", async () => {
    const { salvarSessao, lerSessao } = await import("./sessao");
    const sessao = criarSessao();

    const mock = vi
      .spyOn(Storage.prototype, "setItem")
      .mockImplementation(() => {
        throw new Error("Storage indisponível");
      });

    salvarSessao(sessao);

    expect(lerSessao()).toEqual(sessao);

    mock.mockRestore();
  });

  it("detecta a expiração de uma sessão mantida em memória", async () => {
    const { salvarSessao, lerSessao } = await import("./sessao");
    const agora = Date.now();

    salvarSessao(criarSessao());

    vi.spyOn(Date, "now").mockReturnValue(agora + 7201000);

    expect(lerSessao()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });
});
