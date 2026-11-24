import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form"; // mudou: useWatch
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { devolucaoService } from "../devolucaoService";
import type { ClienteResumo, Devolucao, ItemPendente } from "../types";

const esquema = z.object({
  clienteId: z
    .number({ invalid_type_error: "Selecione o cliente" })
    .int()
    .positive("Selecione o cliente"),
  itemContratoId: z
    .number({ invalid_type_error: "Selecione o item a devolver" })
    .int()
    .positive("Selecione o item a devolver"),
  observacoes: z.string().max(500, "Máximo de 500 caracteres").optional(),
});

type DadosFormulario = z.infer<typeof esquema>;

interface Props {
  onRegistrada: (devolucao: Devolucao) => void;
}

interface ItensDoCliente {
  clienteId: number;
  lista: ItemPendente[];
}

export function RegistrarDevolucaoForm({ onRegistrada }: Props) {
  const [erroEnvio, setErroEnvio] = useState<string | null>(null);
  const [erroCarga, setErroCarga] = useState<string | null>(null);
  const [clientes, setClientes] = useState<ClienteResumo[]>([]);
  const [itens, setItens] = useState<ItensDoCliente | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    resetField,
    control, // mudou: control no lugar de watch
    formState: { errors, isSubmitting },
  } = useForm<DadosFormulario>({ resolver: zodResolver(esquema) });

  const clienteId = useWatch({ control, name: "clienteId" }); // mudou
  const clienteValido = typeof clienteId === "number" && clienteId > 0;
  const opcoesItens =
    clienteValido && itens?.clienteId === clienteId ? itens.lista : [];

  useEffect(() => {
    let ativo = true;
    devolucaoService
      .listarClientes()
      .then((lista) => ativo && setClientes(lista))
      .catch((e: unknown) => ativo && setErroCarga(mensagem(e)));
    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    resetField("itemContratoId");
    if (!clienteValido) return;
    let ativo = true;
    devolucaoService
      .listarItensPendentes(clienteId)
      .then((lista) => ativo && setItens({ clienteId, lista }))
      .catch((e: unknown) => ativo && setErroCarga(mensagem(e)));
    return () => {
      ativo = false;
    };
  }, [clienteId, clienteValido, resetField]);

  async function aoEnviar(dados: DadosFormulario) {
    setErroEnvio(null);
    try {
      const devolucao = await devolucaoService.registrar({
        itemContratoId: dados.itemContratoId,
        clienteId: dados.clienteId,
        observacoes: dados.observacoes?.trim() || undefined,
      });
      onRegistrada(devolucao);
      reset();
    } catch (erro) {
      setErroEnvio(mensagem(erro));
    }
  }

  return (
    <form
      className="dev-formulario"
      onSubmit={handleSubmit(aoEnviar)}
      noValidate
    >
      <label>
        Cliente
        <select
          defaultValue=""
          {...register("clienteId", { valueAsNumber: true })}
        >
          <option value="">Selecione o cliente</option>
          {clientes.map((c) => (
            <option key={c.id} value={c.id}>
              {c.razaoSocial}
            </option>
          ))}
        </select>
        {errors.clienteId && (
          <span className="dev-erro">{errors.clienteId.message}</span>
        )}
      </label>

      <label>
        Item a devolver
        <select
          defaultValue=""
          disabled={!clienteValido}
          {...register("itemContratoId", { valueAsNumber: true })}
        >
          <option value="">
            {clienteValido ? "Selecione o item" : "Escolha o cliente primeiro"}
          </option>
          {opcoesItens.map((i) => (
            <option key={i.itemContratoId} value={i.itemContratoId}>
              {i.descricao} · prevista{" "}
              {new Date(i.dataPrevistaDevolucao).toLocaleDateString("pt-BR")}
            </option>
          ))}
        </select>
        {errors.itemContratoId && (
          <span className="dev-erro">{errors.itemContratoId.message}</span>
        )}
      </label>

      <label className="dev-campo-largo">
        Observações
        <textarea rows={3} {...register("observacoes")} />
        {errors.observacoes && (
          <span className="dev-erro">{errors.observacoes.message}</span>
        )}
      </label>

      {erroCarga && <p className="dev-erro dev-campo-largo">{erroCarga}</p>}
      {erroEnvio && <p className="dev-erro dev-campo-largo">{erroEnvio}</p>}

      <button type="submit" className="dev-botao" disabled={isSubmitting}>
        {isSubmitting ? "Registrando..." : "Registrar devolução"}
      </button>
    </form>
  );
}

function mensagem(erro: unknown): string {
  return erro instanceof Error ? erro.message : "Ocorreu um erro inesperado";
}
