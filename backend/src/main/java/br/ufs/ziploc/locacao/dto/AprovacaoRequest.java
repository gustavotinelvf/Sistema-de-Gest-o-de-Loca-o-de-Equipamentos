package br.ufs.ziploc.locacao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AprovacaoRequest(@NotNull(message = "O percentual de multa é obrigatório.") @DecimalMin(value = "0", message = "O percentual de multa não pode ser negativo.") @DecimalMax(value = "100", message = "O percentual de multa não pode passar de 100.") @Digits(integer = 3, fraction = 2, message = "O percentual de multa deve ter no máximo duas casas decimais.") BigDecimal percentualMulta) {
}
