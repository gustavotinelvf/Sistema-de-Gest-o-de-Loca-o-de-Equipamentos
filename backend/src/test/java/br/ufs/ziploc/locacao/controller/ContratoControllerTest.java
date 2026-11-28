package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.locacao.domain.StatusContrato;
import br.ufs.ziploc.locacao.dto.ContratoListagemResponse;
import br.ufs.ziploc.locacao.service.ContratoService;
import br.ufs.ziploc.shared.seguranca.JwtService;
import br.ufs.ziploc.shared.seguranca.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
        return new ContratoListagemResponse(
                1L, 10L, 7L,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 11),
                status,
                new BigDecimal("1000.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "GERENTE", "FINANCEIRO"})
    void listaContratosPorStatus(String perfil) throws Exception {
        when(contratoService.listar(StatusContrato.ATIVO))
                .thenReturn(List.of(linha(StatusContrato.ATIVO)));

        mvc.perform(get("/api/contratos")
                        .param("status", "ATIVO")
                        .with(user("5").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].clienteId").value(10))
                .andExpect(jsonPath("$[0].solicitacaoId").value(7))
                .andExpect(jsonPath("$[0].dataInicioPrevista").value("2026-12-01"))
                .andExpect(jsonPath("$[0].dataFimPrevista").value("2026-12-11"))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].valorTotal").value(1000.00))
                .andExpect(jsonPath("$[0].itens").doesNotExist());

        verify(contratoService).listar(StatusContrato.ATIVO);
    }

    @Test
    void semStatusListaTodos() throws Exception {
        ContratoListagemResponse planejado = linha(StatusContrato.PLANEJADO);

        ContratoListagemResponse ativo = new ContratoListagemResponse(
                2L, 11L, 8L,
                LocalDate.of(2026, 12, 2),
                LocalDate.of(2026, 12, 12),
                StatusContrato.ATIVO,
                new BigDecimal("1500.00"));

        when(contratoService.listar(null))
                .thenReturn(List.of(planejado, ativo));

        mvc.perform(get("/api/contratos")
                        .with(user("5").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PLANEJADO"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("ATIVO"));

        verify(contratoService).listar(null);
    }

    @ParameterizedTest
    @EnumSource(StatusContrato.class)
    void aceitaTodosOsStatusOficiais(StatusContrato filtro) throws Exception {
        when(contratoService.listar(filtro))
                .thenReturn(List.of(linha(filtro)));

        mvc.perform(get("/api/contratos")
                        .param("status", filtro.name())
                        .with(user("5").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value(filtro.name()));

        verify(contratoService).listar(filtro);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVENTADO", "PENDENTE", "123"})
    void recusaStatusInexistente(String filtro) throws Exception {
        mvc.perform(get("/api/contratos")
                        .param("status", filtro)
                        .with(user("5").roles("GERENTE")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contratoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ESTOQUISTA", "TECNICO"})
    void negaAcessoAOutrosPerfis(String perfil) throws Exception {
        mvc.perform(get("/api/contratos")
                        .with(user("5").roles(perfil)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(contratoService);
    }

    @Test
    void negaListagemSemAutenticacao() throws Exception {
        mvc.perform(get("/api/contratos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(contratoService);
    }

    @Test
    void retornaListaVaziaSemFiltro() throws Exception {
        when(contratoService.listar(null))
                .thenReturn(List.of());

        mvc.perform(get("/api/contratos")
                        .with(user("5").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(contratoService).listar(null);
    }

    @Test
    void retornaListaVaziaComFiltro() throws Exception {
        when(contratoService.listar(StatusContrato.FINALIZADO))
                .thenReturn(List.of());

        mvc.perform(get("/api/contratos")
                        .param("status", "FINALIZADO")
                        .with(user("5").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(contratoService).listar(StatusContrato.FINALIZADO);
    }
}