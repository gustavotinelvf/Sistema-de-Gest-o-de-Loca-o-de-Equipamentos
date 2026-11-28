package br.ufs.ziploc.vistoria.controller;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.acesso.api.UsuarioApi;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.shared.seguranca.JwtService;
import br.ufs.ziploc.shared.seguranca.SecurityConfig;
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.dto.VistoriaDto;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VistoriaController.class)
@Import(SecurityConfig.class)
class VistoriaControllerTest {

    private static final String URL =
            "/api/devolucoes/10/vistoria";

    private static final Instant INICIO =
            Instant.parse("2026-11-28T10:00:00Z");

    private static final String APROVADA =
            """
            {"resultado":"APROVADA"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VistoriaService vistoriaService;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private LocadoraApi locadoraApi;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioApi usuarioApi;

    private void configurarLocadora() {
        when(locadoraApi.obterParametros().id()).thenReturn(1L);
    }

    @Test
    void tecnicoPodeRegistrarVistoriaAprovada() throws Exception {
        configurarLocadora();

        when(vistoriaService.registrar(
                1L, 10L, ResultadoVistoria.APROVADA, null, 7L))
                .thenReturn(new VistoriaDto(
                        20L, 10L, 7L, INICIO,
                        INICIO.plusSeconds(60),
                        ResultadoVistoria.APROVADA, null));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APROVADA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.devolucaoId").value(10))
                .andExpect(jsonPath("$.tecnicoId").value(7))
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.dataConclusao").exists())
                .andExpect(jsonPath("$.resultado").value("APROVADA"));

        verify(vistoriaService).registrar(
                1L, 10L, ResultadoVistoria.APROVADA, null, 7L);
    }

    @Test
    void tecnicoPodeRegistrarVistoriaReprovada() throws Exception {
        configurarLocadora();

        when(vistoriaService.registrar(
                1L, 10L, ResultadoVistoria.REPROVADA,
                "Motor danificado", 7L))
                .thenReturn(new VistoriaDto(
                        20L, 10L, 7L, INICIO,
                        INICIO.plusSeconds(60),
                        ResultadoVistoria.REPROVADA,
                        "Motor danificado"));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resultado":"REPROVADA",
                                  "descricaoAvarias":"Motor danificado"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("REPROVADA"))
                .andExpect(jsonPath("$.descricaoAvarias")
                        .value("Motor danificado"));

        verify(vistoriaService).registrar(
                1L, 10L, ResultadoVistoria.REPROVADA,
                "Motor danificado", 7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GERENTE", "ATENDENTE", "FINANCEIRO", "ESTOQUISTA"
    })
    void outrosPerfisNaoPodemRegistrar(String perfil) throws Exception {
        mvc.perform(post(URL)
                        .with(user("7").roles(perfil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APROVADA))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vistoriaService, locadoraApi);
    }

    @Test
    void usuarioNaoAutenticadoRecebe401() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APROVADA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(vistoriaService, locadoraApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"resultado\":null}",
            "{\"resultado\":\"INVALIDO\"}"
    })
    void resultadoInvalidoRecebe400(String corpo) throws Exception {
        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vistoriaService);
    }

    @Test
    void descricaoAcimaDoLimiteRecebe400() throws Exception {
        String corpo = """
                {"resultado":"REPROVADA","descricaoAvarias":"%s"}
                """.formatted("A".repeat(2001));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vistoriaService);
    }

    @Test
    void devolucaoInexistenteRecebe404() throws Exception {
        configurarLocadora();

        when(vistoriaService.registrar(
                1L, 10L, ResultadoVistoria.APROVADA, null, 7L))
                .thenThrow(new RecursoNaoEncontradoException(
                        "Devolução não encontrada."));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APROVADA))
                .andExpect(status().isNotFound());
    }

    @Test
    void vistoriaJaConcluidaRecebe409() throws Exception {
        configurarLocadora();

        when(vistoriaService.registrar(
                1L, 10L, ResultadoVistoria.APROVADA, null, 7L))
                .thenThrow(new IllegalStateException(
                        "A vistoria já foi concluída."));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APROVADA))
                .andExpect(status().isConflict());
    }

    @Test
    void reprovacaoSemAvariasRecebe422() throws Exception {
        configurarLocadora();

        when(vistoriaService.registrar(
                1L, 10L, ResultadoVistoria.REPROVADA, null, 7L))
                .thenThrow(new IllegalArgumentException(
                        "RD022: descrição obrigatória."));

        mvc.perform(post(URL)
                        .with(user("7").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultado":"REPROVADA"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}