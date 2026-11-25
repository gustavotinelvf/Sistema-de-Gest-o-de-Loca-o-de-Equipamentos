package br.ufs.ziploc.vistoria.dto;

import br.ufs.ziploc.vistoria.domain.StatusVistoria;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarVistoriaRequest(
        @NotNull(message = "O status da vistoria é obrigatório.") StatusVistoria status,
        @PositiveOrZero(message = "O custo do dano não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O custo deve ter no máximo duas casas decimais.")
        BigDecimal custoDano,
        @Size(max = 2000) String laudo) {
}