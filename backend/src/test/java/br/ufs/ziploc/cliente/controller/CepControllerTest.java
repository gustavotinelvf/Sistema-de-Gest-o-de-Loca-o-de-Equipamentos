package br.ufs.ziploc.cliente.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.cliente.dto.CepResponseDTO;
import br.ufs.ziploc.cliente.service.CepInvalidoException;
import br.ufs.ziploc.cliente.service.CepNaoEncontradoException;
import br.ufs.ziploc.cliente.service.ConsultaCepService;
import br.ufs.ziploc.integracao.cep.CepIndisponivelException;
import br.ufs.ziploc.shared.seguranca.JwtService;
import br.ufs.ziploc.shared.seguranca.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CepController.class)
@Import(SecurityConfig.class)
class CepControllerTest {

    private static final String ROTA = "/api/clientes/cep/{cep}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultaCepService consultaCepService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioApi usuarioApi;

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void consultar_perfilAutorizado_retornaEndereco(String perfil) throws Exception {
        when(consultaCepService.consultar("49010000")).thenReturn(
                new CepResponseDTO("49010000", "Rua A", "Centro", "Aracaju", "SE"));

        mvc.perform(get(ROTA, "49010000")
                        .with(user("5").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("49010000"))
                .andExpect(jsonPath("$.logradouro").value("Rua A"))
                .andExpect(jsonPath("$.bairro").value("Centro"))
                .andExpect(jsonPath("$.cidade").value("Aracaju"))
                .andExpect(jsonPath("$.uf").value("SE"));
    }

    @Test
    void consultar_cepInvalido_retorna400() throws Exception {
        when(consultaCepService.consultar("abc49010000"))
                .thenThrow(new CepInvalidoException());

        mvc.perform(get(ROTA, "abc49010000")
                        .with(user("5").roles("ATENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("CEP inválido: informe 8 dígitos"));
    }

    @Test
    void consultar_cepInexistente_retorna404() throws Exception {
        when(consultaCepService.consultar("00000000"))
                .thenThrow(new CepNaoEncontradoException());

        mvc.perform(get(ROTA, "00000000")
                        .with(user("5").roles("ATENDENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("CEP não encontrado"));
    }

    @Test
    void consultar_servicoIndisponivel_retorna503() throws Exception {
        when(consultaCepService.consultar("99999999"))
                .thenThrow(new CepIndisponivelException());

        mvc.perform(get(ROTA, "99999999")
                        .with(user("5").roles("ATENDENTE")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(
                        "Consulta de CEP indisponível no momento: digite o endereço manualmente"));
    }

    @Test
    void consultar_semAutenticacao_retorna401() throws Exception {
        mvc.perform(get(ROTA, "49010000"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(consultaCepService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINANCEIRO", "TECNICO", "ESTOQUISTA"})
    void consultar_perfilNaoAutorizado_retorna403(String perfil) throws Exception {
        mvc.perform(get(ROTA, "49010000")
                        .with(user("5").roles(perfil)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(consultaCepService);
    }
}
