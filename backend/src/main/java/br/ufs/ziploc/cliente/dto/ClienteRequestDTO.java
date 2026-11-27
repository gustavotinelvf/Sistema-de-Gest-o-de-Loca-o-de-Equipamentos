package br.ufs.ziploc.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


//Payload de entrada para cadastro de cliente PJ (RF001)

public record ClienteRequestDTO(
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
        String razaoSocial,

        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj,

        @Size(max = 200, message = "Nome do representante deve ter no máximo 200 caracteres")
        String representanteNome,

        @Pattern(regexp = "^$|^[0-9]{11}$", message = "CPF do representante deve conter 11 dígitos numéricos")
        String representanteCpf,

        @Email(message = "E-mail inválido")
        @Size(max = 200, message = "E-mail deve ter no máximo 200 caracteres")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,

        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "^[0-9]{5}-?[0-9]{3}$", message = "CEP deve conter 8 dígitos numéricos")
        String cep,

        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
        String logradouro,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
        String complemento,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String bairro,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String cidade,

        @NotBlank(message = "UF é obrigatória")
        @Pattern(
                regexp = "^(?i)(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                message = "UF inválida"
        )
        String uf
) {
}