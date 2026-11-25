package br.ufs.ziploc.manutencao.dto;

import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record IniciarManutencaoRequest(
        @NotBlank(message = "O número de série é obrigatório.") @Size(max = 50) String numeroSerie,
        @NotNull(message = "O tipo da manutenção é obrigatório.") TipoManutencao tipo,
        @NotBlank(message = "A descrição é obrigatória.") @Size(max = 2000) String descricao,
        @Future(message = "A data prevista deve ser futura.") Instant dataPrevista) {
}