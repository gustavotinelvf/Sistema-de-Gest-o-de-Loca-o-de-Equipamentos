package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.domain.StatusSolicitacao;
import br.ufs.ziploc.locacao.dto.SolicitacaoResponse;
import br.ufs.ziploc.locacao.dto.SolicitacaoResumo;
import br.ufs.ziploc.locacao.service.SolicitacaoNaoEncontradaException;
import br.ufs.ziploc.locacao.service.SolicitacaoService;
import br.ufs.ziploc.shared.seguranca.JwtService;
import br.ufs.ziploc.shared.seguranca.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitacaoController.class)
@Import(SecurityConfig.class)
class SolicitacaoControllerTest {
    private static final String CONDICOES = "Devolver os equipamentos no prazo contratado.";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SolicitacaoService solicitacaoService;
    @MockitoBean
    private JwtService jwtService;

    private MockHttpServletRequestBuilder reprovar(Long id, String corpo) {
        return post("/api/solicitacoes/{id}/reprovacao", id).with(csrf()).with(user("5").roles("GERENTE")).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private MockHttpServletRequestBuilder aprovar(String corpo) {
        return post("/api/solicitacoes/7/aprovacao").with(csrf()).with(user("5").roles("GERENTE")).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void registraComUsuarioAutenticado() throws Exception {
        SolicitacaoResponse dto = mock(SolicitacaoResponse.class);
        when(dto.id()).thenReturn(7L);
        when(solicitacaoService.registrar(any(), eq(5L))).thenReturn(dto);
        mvc.perform(post("/api/solicitacoes").with(csrf()).with(user("5").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1,\"dataInicioDesejada\":\"2027-12-01\",\"dataFimDesejada\":\"2027-12-10\",\"itens\":[{\"modeloId\":1,\"quantidade\":2}]}")).andExpect(status().isCreated());
        verify(solicitacaoService).registrar(any(), eq(5L));
    }

    @Test
    void aprovaERespondeCriado() throws Exception {
        ContratoDTO dto = new ContratoDTO(70L, 7L, 1L, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), "PLANEJADO", new BigDecimal("1000.00"), new BigDecimal("12.50"), List.of());
        when(solicitacaoService.aprovar(7L, 5L, new BigDecimal("12.50"), CONDICOES)).thenReturn(dto);
        mvc.perform(aprovar("{\"percentualMulta\":12.50,\"condicoesDevolucao\":\"" + CONDICOES + "\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(70)).andExpect(jsonPath("$.status").value("PLANEJADO"));
        verify(solicitacaoService).aprovar(7L, 5L, new BigDecimal("12.50"), CONDICOES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"condicoesDevolucao\":\"Motivo\"}", "{\"percentualMulta\":-1,\"condicoesDevolucao\":\"Motivo\"}", "{\"percentualMulta\":100.01,\"condicoesDevolucao\":\"Motivo\"}", "{\"percentualMulta\":1.234,\"condicoesDevolucao\":\"Motivo\"}", ""})
    void recusaPercentualInvalido(String corpo) throws Exception {
        mvc.perform(aprovar(corpo)).andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacaoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"percentualMulta\":10}", "{\"percentualMulta\":10,\"condicoesDevolucao\":null}", "{\"percentualMulta\":10,\"condicoesDevolucao\":\"\"}", "{\"percentualMulta\":10,\"condicoesDevolucao\":\" \"}"})
    void recusaCondicoesDeDevolucaoInvalidas(String corpo) throws Exception {
        mvc.perform(aprovar(corpo)).andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void respondeConflitoComFaltas() throws Exception {
        when(solicitacaoService.aprovar(any(), any(), any(), anyString())).thenThrow(new IndisponibilidadeException(List.of(new IndisponibilidadeException.Falta(30L, 2, 1))));
        mvc.perform(aprovar("{\"percentualMulta\":10,\"condicoesDevolucao\":\"" + CONDICOES + "\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.faltas[0].modeloId").value(30));
    }

    @Test
    void respondeConflitoQuandoBancoRecusaReserva() throws Exception {
        when(solicitacaoService.aprovar(any(), any(), any(), anyString())).thenThrow(new DataIntegrityViolationException("falha", new SQLException("viola reserva_sem_sobreposicao", "23P01")));
        mvc.perform(aprovar("{\"percentualMulta\":10,\"condicoesDevolucao\":\"" + CONDICOES + "\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Uma das unidades já está reservada no período."));
    }

    @Test
    void reprovaComJustificativa() throws Exception {
        SolicitacaoDTO dto = new SolicitacaoDTO(7L, 1L, 2L, 5L, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), "REJEITADA", "Motivo", List.of());
        when(solicitacaoService.reprovar(7L, 5L, "Motivo")).thenReturn(dto);
        mvc.perform(reprovar(7L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA")).andExpect(jsonPath("$.analisadoPorId").value(5));
    }

    @Test
    void recusaJustificativaEmBranco() throws Exception {
        mvc.perform(reprovar(7L, "{\"justificativa\":\" \"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void respondeNaoEncontrado() throws Exception {
        when(solicitacaoService.reprovar(eq(99L), any(), anyString())).thenThrow(new SolicitacaoNaoEncontradaException(99L));
        mvc.perform(reprovar(99L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isNotFound());
    }

    @Test
    void respondeConflitoQuandoJaAnalisada() throws Exception {
        when(solicitacaoService.reprovar(eq(7L), any(), anyString())).thenThrow(new IllegalStateException("Já analisada."));
        mvc.perform(reprovar(7L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isConflict());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(post("/api/solicitacoes/7/reprovacao").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Motivo\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void respondeRegraVioladaCom422() throws Exception {
        when(solicitacaoService.reprovar(eq(7L), any(), anyString())).thenThrow(new IllegalArgumentException("Regra de negócio violada."));
        mvc.perform(reprovar(7L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().is(422));
    }

    @Test
    void impedeAtendenteDeAprovarSolicitacao() throws Exception {
        mvc.perform(post("/api/solicitacoes/7/aprovacao").with(csrf()).with(user("5").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content("{\"percentualMulta\":10,\"condicoesDevolucao\":\"" + CONDICOES + "\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void impedeAtendenteDeReprovarSolicitacao() throws Exception {
        mvc.perform(post("/api/solicitacoes/7/reprovacao").with(csrf()).with(user("5").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Motivo\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void impedeFinanceiroDeRegistrarSolicitacao() throws Exception {
        mvc.perform(post("/api/solicitacoes").with(csrf()).with(user("5").roles("FINANCEIRO")).contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1,\"dataInicioDesejada\":\"2027-12-01\",\"dataFimDesejada\":\"2027-12-10\",\"itens\":[{\"modeloId\":1,\"quantidade\":2}]}")).andExpect(status().isForbidden());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void listaFilaDeSolicitacoesPendentes_RF010() throws Exception {
        SolicitacaoResumo resumo = new SolicitacaoResumo(7L, 1L, 5L, null, LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 10), new BigDecimal("200.00"), StatusSolicitacao.PENDENTE);
        when(solicitacaoService.listar(StatusSolicitacao.PENDENTE)).thenReturn(List.of(resumo));
        mvc.perform(get("/api/solicitacoes").param("status", "PENDENTE").with(user("5").roles("GERENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7)).andExpect(jsonPath("$[0].status").value("PENDENTE")).andExpect(jsonPath("$[0].valorEstimado").value(200.00));
        verify(solicitacaoService).listar(StatusSolicitacao.PENDENTE);
    }

    @Test
    void listaSemFiltroParaAtendente() throws Exception {
        when(solicitacaoService.listar(null)).thenReturn(List.of());
        mvc.perform(get("/api/solicitacoes").with(user("5").roles("ATENDENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        verify(solicitacaoService).listar(null);
    }

    @Test
    void recusaStatusInexistenteNaFila() throws Exception {
        mvc.perform(get("/api/solicitacoes").param("status", "QUALQUER").with(user("5").roles("GERENTE"))).andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void impedeFinanceiroDeVerAFila() throws Exception {
        mvc.perform(get("/api/solicitacoes").with(user("5").roles("FINANCEIRO"))).andExpect(status().isForbidden());
        verifyNoInteractions(solicitacaoService);
    }
}
