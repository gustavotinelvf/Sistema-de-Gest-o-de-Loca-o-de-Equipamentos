package br.ufs.ziploc.acesso.api;

import br.ufs.ziploc.acesso.domain.Locadora;
import java.math.BigDecimal;

/**
 * Parâmetros da locadora que os outros módulos podem ler (D2/D6: a entidade
 * Locadora nunca sai do módulo acesso). Os campos do endereço levam o sufixo
 * "Sede" para não se confundirem com o endereço do cliente ou da entrega.
 */
public record ParametrosLocadoraDTO(
        Long id,
        String razaoSocial,
        String cnpj,
        String cepSede,
        String logradouroSede,
        String numeroSede,
        String complementoSede,
        String bairroSede,
        String cidadeSede,
        String ufSede,
        BigDecimal raioEntregaKm,
        BigDecimal percentualMultaPadrao) {

    public static ParametrosLocadoraDTO from(Locadora locadora) {
        var e = locadora.getEndereco();
        return new ParametrosLocadoraDTO(
                locadora.getId(),
                locadora.getRazaoSocial(),
                locadora.getCnpj(),
                e.getCep(),
                e.getLogradouro(),
                e.getNumero(),
                e.getComplemento(),
                e.getBairro(),
                e.getCidade(),
                e.getUf(),
                locadora.getRaioEntregaKm(),
                locadora.getPercentualMultaPadrao());
    }
}