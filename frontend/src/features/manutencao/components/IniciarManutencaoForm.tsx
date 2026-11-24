import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { manutencaoService } from "../manutencaoService";

const esquema = z.object({
  numeroSerie: z.string().trim().min(1, "Informe o número de série"),
  descricaoProblema: z.string().trim().min(1, "Descreva o problema"),
});

type DadosFormulario = z.infer<typeof esquema>;

interface Props {
  onIniciada: (numeroSerie: string) => void;
}

export function IniciarManutencaoForm({ onIniciada }: Props) {
  const [erroEnvio, setErroEnvio] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DadosFormulario>({ resolver: zodResolver(esquema) });

  async function aoEnviar(dados: DadosFormulario) {
    setErroEnvio(null);
    try {
      await manutencaoService.iniciar(dados);
      onIniciada(dados.numeroSerie);
      reset();
    } catch (erro) {
      setErroEnvio(
        erro instanceof Error ? erro.message : "Ocorreu um erro inesperado",
      );
    }
  }

  return (
    <form
      className="man-formulario"
      onSubmit={handleSubmit(aoEnviar)}
      noValidate
    >
      <label>
        Número de série
        <input type="text" {...register("numeroSerie")} />
        {errors.numeroSerie && (
          <span className="man-erro">{errors.numeroSerie.message}</span>
        )}
      </label>

      <label className="man-campo-largo">
        Descrição do problema
        <textarea rows={3} {...register("descricaoProblema")} />
        {errors.descricaoProblema && (
          <span className="man-erro">{errors.descricaoProblema.message}</span>
        )}
      </label>

      {erroEnvio && <p className="man-erro man-campo-largo">{erroEnvio}</p>}

      <button type="submit" className="man-botao" disabled={isSubmitting}>
        {isSubmitting ? "Registrando..." : "Iniciar manutenção"}
      </button>
    </form>
  );
}
