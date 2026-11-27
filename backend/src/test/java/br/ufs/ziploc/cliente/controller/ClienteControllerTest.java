package br.ufs.ziploc.cliente.controller;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.ClienteRequestDTO;
import br.ufs.ziploc.cliente.dto.ClienteResponseDTO;
import br.ufs.ziploc.cliente.service.ClienteService;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.seguranca.JwtService;
import br.ufs.ziploc.shared.seguranca.SecurityConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    private static final String PAYLOAD_VALIDO = """
            {
              "razaoSocial": "Construtora Horizonte Ltda",
              "cnpj": "11.222.333/0001-81",
              "representanteNome": "Maria Silva",
              "representanteCpf": "12345678901",
              "email": "maria@construtora.com",
              "telefone": "79999990000",
              "cep": "49000-000",
              "logradouro": "Av. Beira Mar",
              "numero": "100",
              "complemento": "Sala 2",
              "bairro": "Jabotiana",
              "cidade": "Aracaju",
              "uf": "SE"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioApi usuarioApi;

    private ClienteResponseDTO criarResponseValido() {
        return new ClienteResponseDTO(
                10L, 1L, "Construtora Horizonte Ltda", "11222333000181",
                "Maria Silva", "12345678901", "maria@construtora.com", "79999990000",
                "49000000", "Av. Beira Mar", "100", "Sala 2", "Jabotiana", "Aracaju", "SE",
                StatusAdimplencia.ADIMPLENTE, true, Instant.now(), null
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "GERENTE"})
    void deveCadastrarClienteComSucessoRetornando201_RF001(String perfil) throws Exception {
        when(clienteService.cadastrar(any(ClienteRequestDTO.class))).thenReturn(criarResponseValido());

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO)
                        .with(user("1").roles(perfil)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/clientes/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.locadoraId").value(1))
                .andExpect(jsonPath("$.razaoSocial").value("Construtora Horizonte Ltda"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.representanteNome").value("Maria Silva"))
                .andExpect(jsonPath("$.representanteCpf").value("12345678901"))
                .andExpect(jsonPath("$.email").value("maria@construtora.com"))
                .andExpect(jsonPath("$.telefone").value("79999990000"))
                .andExpect(jsonPath("$.cep").value("49000000"))
                .andExpect(jsonPath("$.logradouro").value("Av. Beira Mar"))
                .andExpect(jsonPath("$.numero").value("100"))
                .andExpect(jsonPath("$.complemento").value("Sala 2"))
                .andExpect(jsonPath("$.bairro").value("Jabotiana"))
                .andExpect(jsonPath("$.cidade").value("Aracaju"))
                .andExpect(jsonPath("$.uf").value("SE"))
                .andExpect(jsonPath("$.statusAdimplencia").value("ADIMPLENTE"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.dataCadastro").exists());
    }

    @Test
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(clienteService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ESTOQUISTA", "TECNICO", "FINANCEIRO"})
    void deveRetornar403ParaPerfisSemPermissao(String perfil) throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO)
                        .with(user("2").roles(perfil)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosEstiveremInvalidos() throws Exception {
        String payloadInvalido = """
                {
                  "razaoSocial": "",
                  "cnpj": "",
                  "cep": "123",
                  "logradouro": "",
                  "numero": "",
                  "bairro": "",
                  "cidade": "",
                  "uf": "XX"
                }
                """;

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido)
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.razaoSocial").exists())
                .andExpect(jsonPath("$.campos.cnpj").exists())
                .andExpect(jsonPath("$.campos.cep").exists())
                .andExpect(jsonPath("$.campos.logradouro").exists())
                .andExpect(jsonPath("$.campos.numero").exists())
                .andExpect(jsonPath("$.campos.bairro").exists())
                .andExpect(jsonPath("$.campos.cidade").exists())
                .andExpect(jsonPath("$.campos.uf").exists());

        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar422QuandoCnpjForInvalido_RD007() throws Exception {
        when(clienteService.cadastrar(any(ClienteRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("CNPJ inválido."));

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO.replace("11.222.333/0001-81", "11.222.333/0001-99"))
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("CNPJ inválido."));
    }

    @Test
    void deveRetornar409QuandoCnpjJaEstiverCadastrado_RD008() throws Exception {
        when(clienteService.cadastrar(any(ClienteRequestDTO.class)))
                .thenThrow(new ConflitoException("Já existe um cliente cadastrado com o CNPJ informado."));

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO)
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um cliente cadastrado com o CNPJ informado."));
    }

    @Test
    void deveRetornar400QuandoEmailForInvalido() throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO.replace("maria@construtora.com", "email-invalido"))
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());

        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoCpfDoRepresentanteTiverFormatoInvalido() throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO.replace("12345678901", "123"))
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.representanteCpf").exists());

        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoJsonForMalformado() throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json-invalido")
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(clienteService);
    }

    @Test
    void deveAceitarCadastroSemCamposOpcionais() throws Exception {
        String payloadMinimo = """
                {
                  "razaoSocial": "Construtora Horizonte Ltda",
                  "cnpj": "11222333000181",
                  "cep": "49000000",
                  "logradouro": "Av. Beira Mar",
                  "numero": "100",
                  "bairro": "Jabotiana",
                  "cidade": "Aracaju",
                  "uf": "SE"
                }
                """;

        when(clienteService.cadastrar(any(ClienteRequestDTO.class)))
                .thenReturn(new ClienteResponseDTO(
                        10L, 1L, "Construtora Horizonte Ltda", "11222333000181",
                        null, null, null, null,
                        "49000000", "Av. Beira Mar", "100", null,
                        "Jabotiana", "Aracaju", "SE",
                        StatusAdimplencia.ADIMPLENTE, true, Instant.now(), null
                ));

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadMinimo)
                        .with(user("1").roles("ATENDENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ativo").value(true));
    }
}
