package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import br.ufs.ziploc.locacao.dto.ContratoListagemResponse;
import br.ufs.ziploc.locacao.service.ContratoService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContratoController.class)
@Import(SecurityConfig.class)
class ContratoControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ContratoService contratoService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioApi usuarioApi;

    private ContratoListagemResponse linha(StatusContrato status) {
        return new ContratoListagemResponse(1L, 10L, 7L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 11), status, new BigDecimal("1000.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "GERENTE", "FINANCEIRO"})
    void listaContratosPorStatus(String perfil) throws Exception {
        when(contratoService.listar(StatusContrato.ATIVO)).thenReturn(List.of(linha(StatusContrato.ATIVO)));

        mvc.perform(get("/api/contratos").param("status", "ATIVO").with(user("5").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ATIVO"));
        verify(contratoService).listar(StatusContrato.ATIVO);
    }

    @Test
    void semStatusListaTodos() throws Exception {
        when(contratoService.listar(null)).thenReturn(List.of(linha(StatusContrato.PLANEJADO), linha(StatusContrato.ATIVO)));

        mvc.perform(get("/api/contratos").with(user("5").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void recusaStatusInexistente() throws Exception {
        mvc.perform(get("/api/contratos").param("status", "INVENTADO").with(user("5").roles("GERENTE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(contratoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ESTOQUISTA", "TECNICO"})
    void negaAcessoAOutrosPerfis(String perfil) throws Exception {
        mvc.perform(get("/api/contratos").with(user("5").roles(perfil)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(contratoService);
    }
}
